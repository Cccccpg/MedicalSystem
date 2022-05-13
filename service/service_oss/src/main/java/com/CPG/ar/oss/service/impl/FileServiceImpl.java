package com.CPG.ar.oss.service.impl;

import com.CPG.ar.oss.service.FileService;
import com.CPG.ar.oss.utils.ConstantOssPropertiesUtils;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileServiceImpl implements FileService {

    @Override
    public String upload(MultipartFile file) {
        // Endpoint以华东1（杭州）为例，其它Region请按实际情况填写。
        String endpoint = ConstantOssPropertiesUtils.END_POINT;
        // 阿里云账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM用户进行API访问或日常运维，请登录RAM控制台创建RAM用户。
        String accessKeyId = ConstantOssPropertiesUtils.ACCESS_KEY_ID;
        String accessKeySecret = ConstantOssPropertiesUtils.SECRET;
        // 填写Bucket名称，例如examplebucket。
        String bucketName = ConstantOssPropertiesUtils.BUCKET;

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        InputStream inputStream = null;
        try {
            inputStream = file.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 填写Object完整路径，完整路径中不能包含Bucket名称，例如exampledir/exampleobject.txt。
        String objectName = file.getOriginalFilename();
        //生成随机唯一值，使用uuid，添加到文件名称里
        String uuid = UUID.randomUUID().toString().replaceAll("-","");
        objectName = uuid+objectName;

        //按照当前日期创建文件夹，并将文件上传到创建的文件夹中
        String timeUrl = new DateTime().toString("yyyy/MM/dd");
        objectName = timeUrl+"/"+objectName;

        // 创建PutObject请求,调用方法实现上传。
        ossClient.putObject(bucketName, objectName, inputStream);

        //关闭OSSClient
        ossClient.shutdown();

        //上传后文件路径
        String url = "https://"+bucketName+"."+endpoint+"/"+objectName;

        return url;
    }
}
