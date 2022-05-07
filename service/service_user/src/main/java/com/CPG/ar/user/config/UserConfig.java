package com.CPG.ar.user.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.CPG.ar.user.mapper")
public class UserConfig {

}
