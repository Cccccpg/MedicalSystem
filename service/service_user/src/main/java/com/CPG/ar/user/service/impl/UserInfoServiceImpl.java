package com.CPG.ar.user.service.impl;

import com.CPG.ar.common.exception.AppointmentRegisterException;
import com.CPG.ar.common.helper.JwtHelper;
import com.CPG.ar.common.result.ResultCodeEnum;
import com.CPG.ar.entity.user.Patient;
import com.CPG.ar.entity.user.UserInfo;
import com.CPG.ar.enums.AuthStatusEnum;
import com.CPG.ar.user.mapper.UserInfoMapper;
import com.CPG.ar.user.service.PatientService;
import com.CPG.ar.user.service.UserInfoService;
import com.CPG.ar.vo.user.LoginVo;
import com.CPG.ar.vo.user.UserAuthVo;
import com.CPG.ar.vo.user.UserInfoQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private PatientService patientService;

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

        //绑定手机号码
        UserInfo userInfo = null;
        if(!StringUtils.isEmpty(loginVo.getOpenid())) {
            userInfo = this.selectWxInfoOpenId(loginVo.getOpenid());
            if(null != userInfo) {
                userInfo.setPhone(loginVo.getPhone());
                this.updateById(userInfo);
            } else {
                throw new AppointmentRegisterException(ResultCodeEnum.DATA_ERROR);
            }
        }

        //如果userInfo为空，进行正常手机登录
        if (userInfo == null){
            //4 判断是否为第一次登录：根据手机号查询数据库，如果不存在数据，那么就是第一次登录
            QueryWrapper<UserInfo> wrapper = new QueryWrapper<>();
            wrapper.eq("phone",phone);
            userInfo = baseMapper.selectOne(wrapper);
            if (null == userInfo){  //第一次登录
                //添加信息到数据库中
                userInfo = new UserInfo();
                userInfo.setName("");
                userInfo.setPhone(phone);
                userInfo.setStatus(1);

                baseMapper.insert(userInfo);
            }
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

    //根据openid查询
    @Override
    public UserInfo selectWxInfoOpenId(String openid) {
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("openid",openid);
        UserInfo userInfo = baseMapper.selectOne(queryWrapper);
        return userInfo;
    }

    //用户认证
    @Override
    public void userAuth(Long userId, UserAuthVo userAuthVo) {
        //根据用户id查询用户信息
        UserInfo userInfo = baseMapper.selectById(userId);
        //设置认证信息
        //认证人的姓名
        userInfo.setName(userAuthVo.getName());
        //其他信息
        userInfo.setCertificatesType(userAuthVo.getCertificatesType());
        userInfo.setCertificatesNo(userAuthVo.getCertificatesNo());
        userInfo.setCertificatesUrl(userAuthVo.getCertificatesUrl());
        userInfo.setAuthStatus(AuthStatusEnum.AUTH_RUN.getStatus());

        //进行信息更新
        baseMapper.updateById(userInfo);
    }

    //用户列表（条件查询带分页）
    @Override
    public IPage<UserInfo> selectPage(Page<UserInfo> pageParam, UserInfoQueryVo userInfoQueryVo) {
        //通过UserInfoQueryVo获取条件值

        String name = userInfoQueryVo.getKeyword();             //用户名称
        Integer status = userInfoQueryVo.getStatus();           //用户状态
        Integer authStatus = userInfoQueryVo.getAuthStatus();   //认证状态
        String createTimeBegin = userInfoQueryVo.getCreateTimeBegin();  //开始时间
        String createTimeEnd = userInfoQueryVo.getCreateTimeEnd();  //结束时间

        //对条件值进行非空判断
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(name)){
            queryWrapper.like("name",name);
        }
        if (!StringUtils.isEmpty(status)){
            queryWrapper.eq("status",status);
        }
        if (!StringUtils.isEmpty(authStatus)){
            queryWrapper.eq("auth_status",authStatus);
        }
        if (!StringUtils.isEmpty(createTimeBegin)){
            queryWrapper.ge("create_time",createTimeBegin);
        }
        if (!StringUtils.isEmpty(createTimeEnd)){
            queryWrapper.le("update_time",createTimeEnd);
        }
        //调用mapper中的方法
        Page<UserInfo> userInfoPage = baseMapper.selectPage(pageParam, queryWrapper);
        //把编号变成对应的值
        userInfoPage.getRecords().stream().forEach(item ->{
            this.packageUserInfo(item);
        });

        return userInfoPage;
    }

    //用户锁定
    @Override
    public void lock(Long userId, Integer status) {
        if (status == 0 || status == 1){
            UserInfo userInfo = baseMapper.selectById(userId);
            userInfo.setStatus(status);
            baseMapper.updateById(userInfo);
        }
    }

    //用户详情
    @Override
    public Map<String, Object> show(Long userId) {
        Map<String,Object> map = new HashMap<>();
        //根据userId查询用户信息
        UserInfo userInfo = baseMapper.selectById(userId);
        this.packageUserInfo(userInfo);
        map.put("userInfo",userInfo);
        //根据userId查询就诊人信息
        List<Patient> patientList = patientService.findAllByUserId(userId);
        map.put("patientList",patientList);
        return map;
    }

    //认证审批
    @Override
    public void approval(Long userId, Integer authStatus) {

    }

    private UserInfo packageUserInfo(UserInfo userInfo) {
        //处理认证状态编码
        userInfo.getParam().put("authStatusString",AuthStatusEnum.getStatusNameByStatus(userInfo.getAuthStatus()));
        //处理用户状态 0 1
        String statusString = userInfo.getStatus()==0?"锁定":"正常";
        userInfo.getParam().put("statusString",statusString);
        return userInfo;
    }
}
