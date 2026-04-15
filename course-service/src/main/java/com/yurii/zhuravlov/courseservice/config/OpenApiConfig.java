package com.yurii.zhuravlov.courseservice.config;

import com.yurii.zhuravlov.courseservice.annotation.CurrentUser;
import com.yurii.zhuravlov.utils.OpenApiUtils;
import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    static {
        SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentUser.class);
    }
    @Bean
    public OpenAPI customOpenAPI() {
        return OpenApiUtils.createOpenAPI("Course Service API", "1.0", "API for managing courses");
    }
}
