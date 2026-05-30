package com.yurii.zhuravlov.courseservice.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableFeignClients(basePackages = "com.yurii.zhuravlov.courseservice.client")
@Profile("!integration-test")
public class FeignConfig {
}