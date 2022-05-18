package com.CPG.ar.hosp.service;

import com.CPG.ar.entity.hosp.HospitalSet;
import com.CPG.ar.vo.order.SignInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;


public interface HospitalSetService extends IService<HospitalSet> {

    //根据传过来的医院编码，查询数据库，查询签名
    String getSignKey(String hoscode);

    //获取医院签名信息
    SignInfoVo getSignInfoVo(String hoscode);
}
