package com.CPG.ar.msm.controller;

import com.CPG.ar.common.result.Result;
import com.CPG.ar.msm.service.MsmService;
import com.CPG.ar.msm.utils.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/msm")
public class MsmApiController {

    @Autowired
    private MsmService msmService;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    //发送手机验证码
    @GetMapping("send/{phone}")
    public Result sendCode(@PathVariable String phone){
        //从Redis中获取验证码，如果可以获取到，返回ok
        //key：手机号  value：验证码
        String code = redisTemplate.opsForValue().get(phone);
        if (!StringUtils.isEmpty(code)){
            return Result.ok();
        }

        //如果获取不到，生成验证码，通过整合短信服务进行发送
        code = RandomUtils.getSixBitRandom();
        //整合阿里云短信服务进行发送
        boolean isSend = msmService.send(phone,code);
        //生成的验证码放到redis中，设置有效时间
        if (isSend){
            redisTemplate.opsForValue().set(phone,code,2, TimeUnit.MINUTES);
            return Result.ok();
        }else {
            return Result.fail().message("发送短信失败！");
        }
    }
}
