package com.CPG.ar.order.service.impl;

import com.CPG.ar.common.exception.AppointmentRegisterException;
import com.CPG.ar.common.helper.HttpRequestHelper;
import com.CPG.ar.common.result.ResultCodeEnum;
import com.CPG.ar.common.utils.MD5;
import com.CPG.ar.entity.order.OrderInfo;
import com.CPG.ar.entity.user.Patient;
import com.CPG.ar.enums.OrderStatusEnum;
import com.CPG.ar.hosp.client.HospitalFeignClient;
import com.CPG.ar.order.mapper.OrderMapper;
import com.CPG.ar.order.service.OrderService;
import com.CPG.ar.order.service.WeixinService;
import com.CPG.ar.user.client.PatientFeignClient;
import com.CPG.ar.vo.hosp.ScheduleOrderVo;
import com.CPG.ar.vo.msm.MsmVo;
import com.CPG.ar.vo.order.*;
import com.CPG.common.rabbit.constant.MqConst;
import com.CPG.common.rabbit.service.RabbitService;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderInfo> implements OrderService {

    @Autowired
    private PatientFeignClient patientFeignClient;

    @Autowired
    private HospitalFeignClient hospitalFeignClient;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private WeixinService weixinService;

    //创建挂号订单
    @Override
    public Long saveOrder(String scheduleId, Long patientId) {
        //远程调用service_user中的接口，获取就诊人信息
        Patient patient = patientFeignClient.getPatientOrder(patientId);

        //远程调用service_hosp中的接口，获取排班相关信息
        ScheduleOrderVo scheduleOrderVo = hospitalFeignClient.getScheduleOrderVo(scheduleId);

        //1.判断当前就诊人在当前时间段是否预约过
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getPatientId, patientId);
        queryWrapper.eq(OrderInfo::getReserveDate, scheduleOrderVo.getReserveDate());
        queryWrapper.eq(OrderInfo::getReserveTime, scheduleOrderVo.getReserveTime());
        List<OrderInfo> patientOrderList = baseMapper.selectList(queryWrapper);
        if (patientOrderList.size() != 0){
            throw new AppointmentRegisterException(ResultCodeEnum.REPEAT_SUBMIT_ORDER);
        }

        //2.判断当前时间是否还可以预约
        if(new DateTime(scheduleOrderVo.getStartTime()).isAfterNow()
            || new DateTime(scheduleOrderVo.getEndTime()).isBeforeNow()){
            throw new AppointmentRegisterException(ResultCodeEnum.TIME_NO);
        }

        //获取签名信息
        SignInfoVo signInfoVo = hospitalFeignClient.getSignInfoVo(scheduleOrderVo.getHoscode());

        //添加到订单表汇总
        OrderInfo orderInfo = new OrderInfo();
        //把scheduleOrderVo中数据复制到orderInfo中
        BeanUtils.copyProperties(scheduleOrderVo,orderInfo);
        //向orderInfo中设置其他数据
        String outTradeNo = System.currentTimeMillis() + ""+ new Random().nextInt(100);
        orderInfo.setOutTradeNo(outTradeNo);
        //orderInfo.setScheduleId(scheduleId);
        orderInfo.setScheduleId(scheduleOrderVo.getHosScheduleId());
        orderInfo.setUserId(patient.getUserId());
        orderInfo.setPatientId(patientId);
        orderInfo.setPatientName(patient.getName());
        orderInfo.setPatientPhone(patient.getPhone());
        orderInfo.setOrderStatus(OrderStatusEnum.UNPAID.getStatus());
        baseMapper.insert(orderInfo);

        //调用医院接口，实现预约下单挂号操作
        //设置调用医院接口需要的参数，参数放到map中
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("hoscode",orderInfo.getHoscode());
        paramMap.put("depcode",orderInfo.getDepcode());
        paramMap.put("hosScheduleId",orderInfo.getScheduleId());
        //paramMap.put("ScheduleId",orderInfo.getScheduleId());
        paramMap.put("reserveDate",new DateTime(orderInfo.getReserveDate()).toString("yyyy-MM-dd"));
        paramMap.put("reserveTime", orderInfo.getReserveTime());
        paramMap.put("amount",orderInfo.getAmount());
        paramMap.put("name", patient.getName());
        paramMap.put("certificatesType",patient.getCertificatesType());
        paramMap.put("certificatesNo", patient.getCertificatesNo());
        paramMap.put("sex",patient.getSex());
        paramMap.put("birthdate", patient.getBirthdate());
        paramMap.put("phone",patient.getPhone());
        paramMap.put("isMarry", patient.getIsMarry());
        paramMap.put("provinceCode",patient.getProvinceCode());
        paramMap.put("cityCode", patient.getCityCode());
        paramMap.put("districtCode",patient.getDistrictCode());
        paramMap.put("address",patient.getAddress());
        //联系人
        paramMap.put("contactsName",patient.getContactsName());
        paramMap.put("contactsCertificatesType", patient.getContactsCertificatesType());
        paramMap.put("contactsCertificatesNo",patient.getContactsCertificatesNo());
        paramMap.put("contactsPhone",patient.getContactsPhone());
        paramMap.put("timestamp", HttpRequestHelper.getTimestamp());

        //String sign = HttpRequestHelper.getSign(paramMap, signInfoVo.getSignKey());
        //String sign = MD5.encrypt(signInfoVo.getSignKey());
        //paramMap.put("sign", sign);
        paramMap.put("sign", signInfoVo.getSignKey());

        //请求医院系统中的接口
        //JSONObject result = HttpRequestHelper.sendRequest(paramMap, signInfoVo.getApiUrl() + "/order/submitOrder");
        JSONObject result = HttpRequestHelper.sendRequest(paramMap, "http://localhost:9998/order/submitOrder");

        if (result.getInteger("code") == 200){
            JSONObject jsonObject = result.getJSONObject("data");
            //预约记录唯一标识（医院预约记录主键）
            String hosRecordId = jsonObject.getString("hosRecordId");
            //预约序号
            Integer number = jsonObject.getInteger("number");;
            //取号时间
            String fetchTime = jsonObject.getString("fetchTime");;
            //取号地址
            String fetchAddress = jsonObject.getString("fetchAddress");;
            //更新订单
            orderInfo.setHosRecordId(hosRecordId);
            orderInfo.setNumber(number);
            orderInfo.setFetchTime(fetchTime);
            orderInfo.setFetchAddress(fetchAddress);
            baseMapper.updateById(orderInfo);

            //排班可预约数
            Integer reservedNumber = jsonObject.getInteger("reservedNumber");
            //排班剩余预约数
            Integer availableNumber = jsonObject.getInteger("availableNumber");

            //发送mq信息更新号源和短信通知
            OrderMqVo orderMqVo = new OrderMqVo();
            orderMqVo.setScheduleId(scheduleId);
            orderMqVo.setReservedNumber(reservedNumber);
            orderMqVo.setAvailableNumber(availableNumber);

            //短信提示
            MsmVo msmVo = new MsmVo();
            msmVo.setPhone(orderInfo.getPatientPhone());
            String reserveDate =
                    new DateTime(orderInfo.getReserveDate()).toString("yyyy-MM-dd")
                            + (orderInfo.getReserveTime()==0 ? "上午": "下午");
            Map<String,Object> param = new HashMap<String,Object>(){{
                put("title", orderInfo.getHosname()+"|"+orderInfo.getDepname()+"|"+orderInfo.getTitle());
                put("amount", orderInfo.getAmount());
                put("reserveDate", reserveDate);
                put("name", orderInfo.getPatientName());
                put("quitTime", new DateTime(orderInfo.getQuitTime()).toString("yyyy-MM-dd HH:mm"));
            }};
            msmVo.setParam(param);
            //消息实体封装完成
            orderMqVo.setMsmVo(msmVo);
            //发送
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_ORDER, MqConst.ROUTING_ORDER, orderMqVo);
        }else {
            throw new AppointmentRegisterException(result.getString("message"),ResultCodeEnum.FAIL.getCode());
        }
        return orderInfo.getId();
    }

    //根据订单id查询订单详情
    @Override
    public OrderInfo getOrder(String orderId) {
        OrderInfo orderInfo = baseMapper.selectById(orderId);
        return this.packOrderInfo(orderInfo);
    }

    // 订单列表，条件查询带分页
    @Override
    public IPage<OrderInfo> selectPage(Page<OrderInfo> pageParam, OrderQueryVo orderQueryVo) {
        // 通过OrderInfoQueryVo获取条件值
        String name = orderQueryVo.getKeyword();                //医院名称
        Long patientId = orderQueryVo.getPatientId();           //就诊人id
        String orderStatus = orderQueryVo.getOrderStatus();     //订单状态
        String reserveDate = orderQueryVo.getReserveDate();     //安排日期
        String createTimeBegin = orderQueryVo.getCreateTimeBegin(); //开始时间
        String createTimeEnd = orderQueryVo.getCreateTimeEnd();     //结束时间

        //分别对条件值进行判断
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        if (!StringUtils.isEmpty(name)){
            wrapper.like(OrderInfo::getHosname, name);
        }
        if (!StringUtils.isEmpty(patientId)){
            wrapper.eq(OrderInfo::getPatientId, patientId);
        }
        if (!StringUtils.isEmpty(orderStatus)){
            wrapper.eq(OrderInfo::getOrderStatus, orderStatus);
        }
        if (!StringUtils.isEmpty(reserveDate)){
            wrapper.ge(OrderInfo::getReserveDate, reserveDate);
        }
        if (!StringUtils.isEmpty(createTimeBegin)){
            wrapper.ge(OrderInfo::getCreateTime, createTimeBegin);
        }
        if (!StringUtils.isEmpty(createTimeEnd)){
            wrapper.le(OrderInfo::getCreateTime, createTimeEnd);
        }

        //调用mapper方法
        Page<OrderInfo> pages = baseMapper.selectPage(pageParam, wrapper);
        //编号变成对应值封装
        pages.getRecords().stream().forEach(item -> {
            this.packOrderInfo(item);
        });
        return pages;
    }

    //取消预约
    @Override
    public Boolean cancelOrder(Long orderId) {
        // 1 根据订单id获取订单信息
        OrderInfo orderInfo = baseMapper.selectById(orderId);
        // 2 判断是否可以取消，如果预约时间在当前时间之前，也就是过了取消的时间，就不能取消预约
        DateTime quitTime = new DateTime(orderInfo.getQuitTime());
        if (quitTime.isBeforeNow()){
            throw new AppointmentRegisterException(ResultCodeEnum.CANCEL_ORDER_NO);
        }
        // 3 调用医院接口实现取消预约
        SignInfoVo signInfoVo = hospitalFeignClient.getSignInfoVo(orderInfo.getHoscode());
        if(null == signInfoVo) {
            throw new AppointmentRegisterException(ResultCodeEnum.PARAM_ERROR);
        }
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("hoscode",orderInfo.getHoscode());
        reqMap.put("hosRecordId",orderInfo.getHosRecordId());
        reqMap.put("timestamp", HttpRequestHelper.getTimestamp());
        //String sign = HttpRequestHelper.getSign(reqMap, signInfoVo.getSignKey());
        reqMap.put("sign", signInfoVo.getSignKey());
        reqMap.put("hosScheduleId",orderInfo.getScheduleId());

        //JSONObject result = HttpRequestHelper.sendRequest(reqMap, signInfoVo.getApiUrl()+"/order/updateCancelStatus");
        JSONObject result = HttpRequestHelper.sendRequest(reqMap, "http://localhost:9998/order/updateCancelStatus");
        // 4 根据医院接口返回数据
        if (result.getInteger("code") != 200){
            throw new AppointmentRegisterException(result.getString("message"), ResultCodeEnum.FAIL.getCode());
        }else{
            //当前订单是否可以退款
            //如果当前订单状态是已支付，那就退款
            if (orderInfo.getOrderStatus().intValue() == OrderStatusEnum.PAID.getStatus().intValue()){
                Boolean isRefund = weixinService.refund(orderId);
                if (!isRefund){
                    throw new AppointmentRegisterException(ResultCodeEnum.CANCEL_ORDER_FAIL);
                }
                //更新订单状态
                orderInfo.setOrderStatus(OrderStatusEnum.CANCLE.getStatus());
                baseMapper.updateById(orderInfo);

                //发送mq更新预约数量
                OrderMqVo orderMqVo = new OrderMqVo();
                orderMqVo.setScheduleId(orderInfo.getScheduleId());

                //短信提示
                MsmVo msmVo = new MsmVo();
                msmVo.setPhone(orderInfo.getPatientPhone());
                String reserveDate = new DateTime(orderInfo.getReserveDate()).toString("yyyy-MM-dd") + (orderInfo.getReserveTime()==0 ? "上午": "下午");
                Map<String,Object> param = new HashMap<String,Object>(){{
                    put("title", orderInfo.getHosname()+"|"+orderInfo.getDepname()+"|"+orderInfo.getTitle());
                    put("reserveDate", reserveDate);
                    put("name", orderInfo.getPatientName());
                }};
                msmVo.setParam(param);
                orderMqVo.setMsmVo(msmVo);
                rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_ORDER, MqConst.ROUTING_ORDER, orderMqVo);
            }else if(orderInfo.getOrderStatus().intValue() == OrderStatusEnum.UNPAID.getStatus().intValue()){
                //如果当前订单状态是已预约，未支付，那么就更新数据库中订单的状态，不退款
                //更新订单状态为：取消预约
                orderInfo.setOrderStatus(OrderStatusEnum.CANCLE.getStatus());
                baseMapper.updateById(orderInfo);
            }
            return true;
        }
    }

    //实现就诊通知
    @Override
    public void patientTips() {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        //就诊时间等于当前时间的订单才提醒，也即就诊当天早上八点提醒
        wrapper.eq(OrderInfo::getCreateTime, new DateTime().toString("yyyy-MM-dd"));
        //这里只有取消支付的才不提醒就医
        wrapper.ne(OrderInfo::getOrderStatus, OrderStatusEnum.CANCLE.getStatus());
        List<OrderInfo> orderInfoList = baseMapper.selectList(wrapper);
        //2.遍历订单信息，发送提示信息
        for(OrderInfo orderInfo:orderInfoList) {
            //mq发送短信提示
            MsmVo msmVo = new MsmVo();
            msmVo.setPhone(orderInfo.getPatientPhone());
            String reserveDate = new DateTime(orderInfo.getReserveDate()).toString("yyyy-MM-dd")
                    + (orderInfo.getReserveTime()==0 ? "上午": "下午");
            Map<String,Object> param = new HashMap<String,Object>(){{
                put("title", orderInfo.getHosname()+"|"+orderInfo.getDepname()+"|"+orderInfo.getTitle());
                put("reserveDate", reserveDate);
                put("name", orderInfo.getPatientName());
            }};
            msmVo.setParam(param);//在就诊通知中封装了订单信息
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_MSM, MqConst.ROUTING_MSM_ITEM, msmVo);
        }
    }

    //预约统计
    @Override
    public Map<String, Object> getCountMap(OrderCountQueryVo orderCountQueryVo) {
        //1 调用mapper方法得到数据
        List<OrderCountVo> orderCountVoList = baseMapper.selectOrderCount(orderCountQueryVo);

        //2 获取x轴需要的数据，日期数据，list集合
        List<String> dateList = orderCountVoList.stream().map(OrderCountVo::getReserveDate).collect(Collectors.toList());

        //3 获取y轴需要的数据，具体数量，list集合
        List<Integer> countList = orderCountVoList.stream().map(OrderCountVo::getCount).collect(Collectors.toList());

        Map<String, Object> map = new HashMap<>();
        map.put("dateList", dateList);
        map.put("countList", countList);
        return map;
    }

    private OrderInfo packOrderInfo(OrderInfo orderInfo){
        orderInfo.getParam().put("orderStatusString", OrderStatusEnum.getStatusNameByStatus(orderInfo.getOrderStatus()));
        return orderInfo;
    }
}
