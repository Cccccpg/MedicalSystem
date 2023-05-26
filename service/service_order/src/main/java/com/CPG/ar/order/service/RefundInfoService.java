package com.CPG.ar.order.service;

import com.CPG.ar.entity.order.PaymentInfo;
import com.CPG.ar.entity.order.RefundInfo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface RefundInfoService extends IService<RefundInfo> {

    //保存退款记录
    RefundInfo saveRefundInfo(PaymentInfo paymentInfo);
}
