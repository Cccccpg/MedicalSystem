package com.CPG.ar.user.service.impl;

import com.CPG.ar.common.exception.AppointmentRegisterException;
import com.CPG.ar.common.helper.JwtHelper;
import com.CPG.ar.common.result.ResultCodeEnum;
import com.CPG.ar.entity.user.UserInfo;
import com.CPG.ar.user.mapper.UserInfoMapper;
import com.CPG.ar.user.service.UserInfoService;
import com.CPG.ar.vo.user.LoginVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    //用户手机号登录接口
    @Override
    public Map<String, Object> loginUser(LoginVo loginVo) {
        //1 从loginVo中获取用户输入的手机号 和 验证码
        String phone = loginVo.getPhone();
        String code = loginVo.getCode();

        //2 判断值是否为空
        if(StringUtils.isEmpty(phone) || StringUtils.isEmpty(code)){
            throw new AppointmentRegisterException(ResultCodeEnum.PARAM_ERROR);
        }

        //3 判断手机收到的验证码与输入的验证码是否正确
        String redisCode = redisTemplate.opsForValue().get(phone);
        if(!code.equals(redisCode)){
            throw new AppointmentRegisterException(ResultCodeEnum.CODE_ERROR);
        }

        //4 判断是否为第一次登录：根据手机号查询数据库，如果不存在数据，那么就是第一次登录
        QueryWrapper<UserInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("phone",phone);
        UserInfo userInfo = baseMapper.selectOne(wrapper);
        if (null == userInfo){  //第一次登录
            //添加信息到数据库中
            userInfo = new UserInfo();
            userInfo.setName("");
            userInfo.setPhone(phone);
            userInfo.setStatus(1);

            baseMapper.insert(userInfo);
        }

        //校验用户是否被禁用
        if (userInfo.getStatus() == 0){
            throw new AppointmentRegisterException(ResultCodeEnum.LOGIN_DISABLED_ERROR);
        }

        //5 不是第一次登陆，就直接登录
        //6 返回登录信息
        //7 返回用户登录名
        //8 返回token信息
        HashMap<String , Object> map = new HashMap<>();
        String name = userInfo.getName();
        if (StringUtils.isEmpty(name)){
            name = userInfo.getNickName();
        }
        if (StringUtils.isEmpty(name)){
            name = userInfo.getPhone();
        }
        map.put("name",name);

        //使用Jwt生成token
        String token = JwtHelper.createToken(userInfo.getId(), name);
        map.put("token",token);
        return map;
    }
}
