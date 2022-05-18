package com.CPG.ar.order.service;

import com.CPG.ar.entity.order.OrderInfo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface OrderService extends IService<OrderInfo> {

    //创建挂号订单
    Long saveOrder(String scheduleId, Long patientId);
}
