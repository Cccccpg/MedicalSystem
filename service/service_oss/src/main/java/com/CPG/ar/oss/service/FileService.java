package com.CPG.ar.oss.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    //文件上传到阿里云oss
    String upload(MultipartFile file);
}
