package com.CPG.ar.msm.service;

import com.CPG.ar.vo.msm.MsmVo;

public interface MsmService {

    //发送手机验证码
    boolean send(String phone, String code);

    //使用发送短信
    boolean send(MsmVo msmVo);
}
