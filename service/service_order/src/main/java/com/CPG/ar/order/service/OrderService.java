package com.CPG.ar.order.service;

import com.CPG.ar.entity.order.OrderInfo;
import com.CPG.ar.vo.order.OrderCountQueryVo;
import com.CPG.ar.vo.order.OrderQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface OrderService extends IService<OrderInfo> {

    //创建挂号订单
    Long saveOrder(String scheduleId, Long patientId);

    // 根据订单id查询订单详情
    OrderInfo getOrder(String orderId);

    // 订单列表，条件查询带分页
    IPage<OrderInfo> selectPage(Page<OrderInfo> pageParam, OrderQueryVo orderQueryVo);

    //取消预约
    Boolean cancelOrder(Long orderId);

    //就诊通知
    void patientTips();

    //预约统计
    Map<String, Object> getCountMap(OrderCountQueryVo orderCountQueryVo);
}
