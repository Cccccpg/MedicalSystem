package com.CPG.ar.order.service;

import com.CPG.ar.entity.order.OrderInfo;
import com.CPG.ar.entity.order.PaymentInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface PaymentService extends IService<PaymentInfo> {

    //向支付记录表中添加信息
    void savePaymentInfo(OrderInfo order, Integer status);

    //更新订单状态
    void paySuccess(String out_trade_no, Map<String, String> resultMap);

    //获取支付记录
    PaymentInfo getPaymentInfo(Long orderId, Integer paymentType);
}
