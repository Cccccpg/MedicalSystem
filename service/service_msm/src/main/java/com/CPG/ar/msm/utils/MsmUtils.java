package com.CPG.ar.msm.utils;

import org.apache.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class MsmUtils {

    /**
     *  发送验证码
     * @param mobile  手机号
     * @param code    发送的验证码
     */
    public static void sendMessage(String mobile,String code){
        String host = "https://dfsns.market.alicloudapi.com";
        String path = "/data/send_sms";
        String method = "POST";
        String appcode = "03e878fdb9054763afe0143f50b3526a";
        Map<String, String> headers = new HashMap<String, String>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);
        //根据API的要求，定义相对应的Content-Type
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        Map<String, String> querys = new HashMap<String, String>();
        Map<String, String> bodys = new HashMap<String, String>();
        bodys.put("content", "code:"+ code +",expire_at:2");
        bodys.put("phone_number", mobile);
        bodys.put("template_id", "TPL_0001");

        try {
            HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
            System.out.println(response.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
