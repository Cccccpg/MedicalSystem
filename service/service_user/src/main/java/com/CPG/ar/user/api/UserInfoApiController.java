package com.CPG.ar.user.api;

import com.CPG.ar.common.result.Result;
import com.CPG.ar.common.utils.AuthContextHolder;
import com.CPG.ar.entity.user.UserInfo;
import com.CPG.ar.user.service.UserInfoService;
import com.CPG.ar.vo.user.LoginVo;
import com.CPG.ar.vo.user.UserAuthVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserInfoApiController {

    @Autowired
    private UserInfoService userInfoService;

    /**
     * 用户手机登录接口
     * @param loginVo
     * @return
     */
    @PostMapping("login")
    public Result login(@RequestBody LoginVo loginVo){
        Map<String, Object> userInfo = userInfoService.loginUser(loginVo);
        return Result.ok(userInfo);
    }

    /**
     * 用户认证接口
     * @param userAuthVo
     * @param request
     * @return
     */
    @PostMapping("auth/userAuth")
    public Result userAuth(@RequestBody UserAuthVo userAuthVo, HttpServletRequest request){
        //在方法中传递两个参数，id 和 认证数据的vo对象
        userInfoService.userAuth(AuthContextHolder.getUserId(request),userAuthVo);
        return Result.ok();
    }

    /**
     * 获取用户id信息接口
     * @param request
     * @return
     */
    @GetMapping("auth/getUserInfo")
    public Result getUserInfo(HttpServletRequest request){
        Long userId = AuthContextHolder.getUserId(request);
        UserInfo userInfo = userInfoService.getById(userId);
        return Result.ok(userInfo);
    }
}
