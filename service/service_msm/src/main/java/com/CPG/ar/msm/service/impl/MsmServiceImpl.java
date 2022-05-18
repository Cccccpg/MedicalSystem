package com.CPG.ar.msm.service.impl;

import com.CPG.ar.msm.service.MsmService;
import com.CPG.ar.msm.utils.MsmUtils;
import com.CPG.ar.vo.msm.MsmVo;
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
        //用阿里云短信服务，设置相关参数
        MsmUtils.sendMessage(phone,code);
        return true;
    }

    //mq发送短信的封装
    @Override
    public boolean send(MsmVo msmVo) {
        //判断手机号是否为空
        if (!StringUtils.isEmpty(msmVo.getPhone())){
            boolean isSend = this.send(msmVo.getPhone(),(String)msmVo.getParam().get("code"));
            return isSend;
        }
        return false;
    }
}
