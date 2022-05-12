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
        try {
            HashMap<String, Object> map = new HashMap<>();
            map.put("appid", ConstantWxPropertiesUtils.WX_OPEN_APP_ID);
            map.put("scope","snsapi_login");
            String wxOpenRedirectUrl = ConstantWxPropertiesUtils.WX_OPEN_REDIRECT_URL;
            wxOpenRedirectUrl = URLEncoder.encode(wxOpenRedirectUrl, "utf-8");
            map.put("redirectUri",wxOpenRedirectUrl);
            map.put("state",System.currentTimeMillis()+"");
            return Result.ok(map);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    //微信扫码后回调方法
    @GetMapping("callback")
    public String callBack(String code, String state){

        if (StringUtils.isEmpty(state) || StringUtils.isEmpty(code)) {
            log.error("非法回调请求");
            throw new AppointmentRegisterException(ResultCodeEnum.ILLEGAL_CALLBACK_REQUEST_ERROR);
        }

        //使用code和appid以及appscrect换取access_token
        StringBuffer baseAccessTokenUrl = new StringBuffer()
                .append("https://api.weixin.qq.com/sns/oauth2/access_token")
                .append("?appid=%s")
                .append("&secret=%s")
                .append("&code=%s")
                .append("&grant_type=authorization_code");

        String accessTokenUrl = String.format(baseAccessTokenUrl.toString(),
                ConstantWxPropertiesUtils.WX_OPEN_APP_ID,
                ConstantWxPropertiesUtils.WX_OPEN_APP_SECRET,
                code);

        //使用httpClient请求这个地址
        try {
            String accessTokenInfo = HttpClientUtils.get(accessTokenUrl);
            //从返回的json数据中 拿到access_token 和 openid
            JSONObject jsonObject = JSONObject.parseObject(accessTokenInfo);
            String access_token = jsonObject.getString("access_token");
            String openid = jsonObject.getString("openid");

            //根据openid判断数据库中是否存在扫码人的信息
            UserInfo userInfo = userInfoService.selectWxInfoOpenId(openid);
            if(userInfo == null){
                //通过两个值获取扫码人的信息
                String baseUserInfoUrl = "https://api.weixin.qq.com/sns/userinfo" +
                        "?access_token=%s" +
                        "&openid=%s";
                String userInfoUrl = String.format(baseUserInfoUrl, access_token, openid);
                String resultInfo = HttpClientUtils.get(userInfoUrl);
                JSONObject resultUserInfoJson = JSONObject.parseObject(resultInfo);
                //解析用户昵称和头像
                String nickname = resultUserInfoJson.getString("nickname");
                String headimgurl = resultUserInfoJson.getString("headimgurl");
                //获取扫码人的信息添加到数据库中
                userInfo = new UserInfo();
                userInfo.setNickName(nickname);
                userInfo.setOpenid(openid);
                userInfo.setStatus(1);

                userInfoService.save(userInfo);
            }

            //返回name和token字符串
            Map<String, String> map = new HashMap<>();
            String name = userInfo.getName();
            if(StringUtils.isEmpty(name)) {
                name = userInfo.getNickName();
            }
            if(StringUtils.isEmpty(name)) {
                name = userInfo.getPhone();
            }
            map.put("name", name);
            //判断userInfo中是否有手机号，如果为空，则返回openid
            //如果不为空，则返回openid值是空字符串
            //前端判断：如果openid不为空，绑定手机号，如果openid为空，则不需要绑定手机号
            if(StringUtils.isEmpty(userInfo.getPhone())) {
                map.put("openid", userInfo.getOpenid());
            } else {
                map.put("openid", "");
            }
            //使用JWT生成token字符串
            String token = JwtHelper.createToken(userInfo.getId(), name);
            map.put("token", token);
            //跳转到前端页面中
            return "redirect:" + ConstantWxPropertiesUtils.AR_BASE_URL + "/weixin/callback?token="+map.get("token")+"&openid="+map.get("openid")+"&name="+ URLEncoder.encode(map.get("name"),"utf-8");

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
