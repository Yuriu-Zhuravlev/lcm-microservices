package com.yurii.zhuravlov.authservice.config;

import com.yurii.zhuravlov.utils.OpenApiUtils;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return OpenApiUtils.createOpenAPI("Course Service API", "1.0", "API for managing courses");

    }
}