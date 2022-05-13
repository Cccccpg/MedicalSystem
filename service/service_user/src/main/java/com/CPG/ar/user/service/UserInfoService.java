package com.CPG.ar.user.service;

import com.CPG.ar.entity.user.UserInfo;
import com.CPG.ar.vo.user.LoginVo;
import com.CPG.ar.vo.user.UserAuthVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface UserInfoService extends IService<UserInfo> {

    //用户手机号登录接口
    Map<String, Object> loginUser(LoginVo loginVo);

    //根据openid查询
    UserInfo selectWxInfoOpenId(String openid);

    //用户认证
    void userAuth(Long userId, UserAuthVo userAuthVo);
}
