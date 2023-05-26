package com.CPG.ar.order.service.impl;

import com.CPG.ar.entity.order.OrderInfo;
import com.CPG.ar.entity.order.PaymentInfo;
import com.CPG.ar.entity.order.RefundInfo;
import com.CPG.ar.enums.PaymentStatusEnum;
import com.CPG.ar.enums.PaymentTypeEnum;
import com.CPG.ar.enums.RefundStatusEnum;
import com.CPG.ar.order.service.OrderService;
import com.CPG.ar.order.service.PaymentService;
import com.CPG.ar.order.service.RefundInfoService;
import com.CPG.ar.order.service.WeixinService;
import com.CPG.ar.order.utils.ConstantPropertiesUtils;
import com.CPG.ar.order.utils.HttpClient;
import com.alibaba.fastjson.JSONObject;
import com.github.wxpay.sdk.WXPayConstants;
import com.github.wxpay.sdk.WXPayUtil;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class WeixinServiceImpl implements WeixinService {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RefundInfoService refundInfoService;

    //生成微信支付二维码
    @Override
    public Map createNative(Long orderId) {
        try {
            // 先从redis中获取数据，如果有那就不用生成
            Map payMap = (Map)redisTemplate.opsForValue().get(orderId.toString());
            if (payMap != null){
                return payMap;
            }

            // 1.根据orderId获取订单信息
            OrderInfo order = orderService.getById(orderId);
            // 2.向支付记录表中添加信息
            paymentService.savePaymentInfo(order, PaymentTypeEnum.WEIXIN.getStatus());
            // 3.设置参数，调用微信生成二维码接口
            // 将参数转换为xml格式，使用商户的key进行加密
            Map paramMap = new HashMap();
            paramMap.put("appid", ConstantPropertiesUtils.APPID);
            paramMap.put("mch_id", ConstantPropertiesUtils.PARTNER);
            paramMap.put("nonce_str", WXPayUtil.generateNonceStr());
            String body = order.getReserveDate() + "就诊"+ order.getDepname();
            paramMap.put("body", body);
            paramMap.put("out_trade_no", order.getOutTradeNo());        //订单编号
            //paramMap.put("total_fee", order.getAmount().multiply(new BigDecimal("100")).longValue()+"");
            paramMap.put("total_fee", "1");                             //订单金额，为了测试，统一写成这个值
            paramMap.put("spbill_create_ip", "127.0.0.1");              //当前ip地址
            paramMap.put("notify_url", "http://guli.shop/api/order/weixinPay/weixinNotify");
            paramMap.put("trade_type", "NATIVE");                       //支付类型
            // 4.调用微信生成二维码接口，httpclient调用
            HttpClient httpClient = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");
            //设置map参数
            httpClient.setXmlParam(WXPayUtil.generateSignedXml(paramMap, ConstantPropertiesUtils.PARTNERKEY));
            //设置支持https的请求方式
            httpClient.setHttps(true);
            //发送
            httpClient.post();

            // 5.返回相关数据
            String xml = httpClient.getContent();
            //将xml转换成map集合
            Map<String, String> resultMap = WXPayUtil.xmlToMap(xml);
            //System.out.println("resultMap:" + resultMap);
            // 6.封装返回结果集
            Map map = new HashMap<>();
            map.put("orderId", orderId);
            map.put("totalFee", order.getAmount());
            map.put("resultCode", resultMap.get("result_code"));
            map.put("codeUrl", resultMap.get("code_url"));      //二维码地址

            //将数据放到redis中
            if (resultMap.get("result_code") != null){
                //设置存放在redis中的时间为120分钟
                redisTemplate.opsForValue().set(orderId.toString(), map, 120, TimeUnit.MINUTES);
            }
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //调用微信接口查询订单状态
    @Override
    public Map<String, String> queryPayStatus(Long orderId) {
        try {
            // 1 根据orderId获取订单信息
            OrderInfo orderInfo = orderService.getById(orderId);

            // 2 封装提交参数
            Map paramMap = new HashMap();
            paramMap.put("appid", ConstantPropertiesUtils.APPID);
            paramMap.put("mch_id", ConstantPropertiesUtils.PARTNER);
            paramMap.put("out_trade_no", orderInfo.getOutTradeNo());
            paramMap.put("nonce_str", WXPayUtil.generateNonceStr());

            // 3 设置请求中的内容
            HttpClient httpClient = new HttpClient("https://api.mch.weixin.qq.com/pay/orderquery");
            httpClient.setXmlParam(WXPayUtil.generateSignedXml(paramMap, ConstantPropertiesUtils.PARTNERKEY));
            httpClient.setHttps(true);
            httpClient.post();

            // 4 得到微信接口返回的数据
            String xml = httpClient.getContent();
            Map<String, String> resultMap = WXPayUtil.xmlToMap(xml);
            //System.out.println("支付状态的resultMap:" + resultMap);

            // 5 把接口数据返回
            return resultMap;

        }catch (Exception e){
            return null;
        }
    }

    //微信退款
    @Override
    public Boolean refund(Long orderId) {
        try {
            //获取支付记录信息
            PaymentInfo paymentInfo = paymentService.getPaymentInfo(orderId, PaymentTypeEnum.WEIXIN.getStatus());
            //将信息加到退款表中
            RefundInfo refundInfo = refundInfoService.saveRefundInfo(paymentInfo);
            //判断，是否已经退款了
            if (refundInfo.getRefundStatus().intValue() == RefundStatusEnum.REFUND.getStatus().intValue()){
                return true;
            }
            //调用微信接口，实现退款
            //封装需要的参数
            Map<String, String> paramMap = new HashMap<>();
            paramMap.put("appid",ConstantPropertiesUtils.APPID);        //公众账号ID
            paramMap.put("mch_id",ConstantPropertiesUtils.PARTNER);     //商户编号
            paramMap.put("nonce_str",WXPayUtil.generateNonceStr());
            paramMap.put("transaction_id",paymentInfo.getTradeNo());    //微信订单号
            paramMap.put("out_trade_no",paymentInfo.getOutTradeNo());   //商户订单编号
            paramMap.put("out_refund_no","tk"+paymentInfo.getOutTradeNo()); //商户退款单号
            //实际开发中的退款金额需要根据付款表中的金额进行退款，但是在该项目中，为了简单起见，就设置为1分钱
//          paramMap.put("total_fee",paymentInfoQuery.getTotalAmount().multiply(new BigDecimal("100")).longValue()+"");
//          paramMap.put("refund_fee",paymentInfoQuery.getTotalAmount().multiply(new BigDecimal("100")).longValue()+"");
            paramMap.put("total_fee","1");
            paramMap.put("refund_fee","1");
            String paramXml = WXPayUtil.generateSignedXml(paramMap,ConstantPropertiesUtils.PARTNERKEY);

            //设置调用的接口内容
            HttpClient httpClient = new HttpClient("https://api.mch.weixin.qq.com/secapi/pay/refund");
            httpClient.setXmlParam(paramXml);
            httpClient.setHttps(true);

            //设置证书信息
            httpClient.setCert(true);
            httpClient.setCertPassword(ConstantPropertiesUtils.PARTNER);
            httpClient.post();

            //接收返回数据
            String xml = httpClient.getContent();
            Map<String, String> resultMap = WXPayUtil.xmlToMap(xml);
            //判断退款是否成功
            if (null != resultMap && WXPayConstants.SUCCESS.equalsIgnoreCase(resultMap.get("result_code"))) {
                //更新退款记录表
                refundInfo.setCallbackTime(new Date());
                refundInfo.setTradeNo(resultMap.get("refund_id"));
                refundInfo.setRefundStatus(RefundStatusEnum.REFUND.getStatus());
                refundInfo.setCallbackContent(JSONObject.toJSONString(resultMap));
                refundInfoService.updateById(refundInfo);
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
