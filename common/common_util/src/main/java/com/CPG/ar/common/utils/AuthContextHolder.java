package com.CPG.ar.common.utils;

import com.CPG.ar.common.helper.JwtHelper;

import javax.servlet.http.HttpServletRequest;

//获取当前用户信息工具类
public class AuthContextHolder {

    //获取当前用户id
    public static Long getUserId(HttpServletRequest request){
        //先从header中获取token
        String token = request.getHeader("token");
        //jwt从token中获取userid
        Long userId = JwtHelper.getUserId(token);
        return userId;
    }

    //获取当前用户名称
    public static String getUserName(HttpServletRequest request){
        //先从header中获取token
        String token = request.getHeader("token");
        //jwt从token中获取userid
        String userName = JwtHelper.getUserName(token);
        return userName;
    }

}
