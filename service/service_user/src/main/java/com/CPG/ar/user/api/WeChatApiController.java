package com.CPG.ar.user.api;

import com.CPG.ar.common.exception.AppointmentRegisterException;
import com.CPG.ar.common.helper.JwtHelper;
import com.CPG.ar.common.result.Result;
import com.CPG.ar.common.result.ResultCodeEnum;
import com.CPG.ar.entity.user.UserInfo;
import com.CPG.ar.user.service.UserInfoService;
import com.CPG.ar.user.utils.ConstantWxPropertiesUtils;
import com.CPG.ar.user.utils.HttpClientUtils;
import com.alibaba.fastjson.JSONObject;
import com.sun.deploy.net.URLEncoder;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

@Api(tags = "微信操作接口")
@Controller
@RequestMapping("/api/ucenter/wx")
@Slf4j
public class WeChatApiController {

    @Autowired
    private UserInfoService userInfoService;

    //生成微信扫描二维码
    /**
     * 返回生成二维码需要的参数
     * @return
     */
    @GetMapping("getLoginParam")
    @ResponseBody
    public Result genQrConnect(){
        HashMap<String, Object> map = userInfoService.getQrConnet();
        return Result.ok(map);
    }

    //微信扫码后回调方法
    @GetMapping("callback")
    public String callBack(String code, String state){
        String result = userInfoService.callBack(code, state);
        return result;
    }
}
