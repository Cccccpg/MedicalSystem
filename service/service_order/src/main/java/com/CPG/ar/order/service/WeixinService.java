package com.CPG.ar.order.service;

import java.util.Map;

public interface WeixinService {
    // 生成微信支付二维码
    Map createNative(Long orderId);

    //调用微信接口查询订单状态
    Map<String, String> queryPayStatus(Long orderId);

    //退款
    Boolean refund(Long orderId);
}
