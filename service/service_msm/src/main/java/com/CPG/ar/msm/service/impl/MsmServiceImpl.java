package com.CPG.ar.msm.service.impl;

import com.CPG.ar.msm.service.MsmService;
import com.CPG.ar.msm.utils.MsmUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MsmServiceImpl implements MsmService {

    //发送手机验证码
    @Override
    public boolean send(String phone, String code) {
        //判断手机号是否为空
        if (StringUtils.isEmpty(phone)){
            return false;
        }
        //整合阿里云短信服务，设置相关参数
        MsmUtils.sendMessage(phone,code);
        return true;
    }
}
