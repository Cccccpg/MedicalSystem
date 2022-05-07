package com.CPG.ar.user.controller;

import com.CPG.ar.common.result.Result;
import com.CPG.ar.user.service.UserInfoService;
import com.CPG.ar.vo.user.LoginVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserInfoApiController {

    @Autowired
    private UserInfoService userInfoService;

    /**
     * 用户手机号登录接口
     * @return
     */
    @PostMapping("login")
    public Result login(@RequestBody LoginVo loginVo){
        Map<String, Object> userInfo = userInfoService.loginUser(loginVo);
        return Result.ok(userInfo);
    }
}
