package com.yurii.zhuravlov.learningservice.config;

import com.yurii.zhuravlov.learningservice.config.annotation.CurrentUser;
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
        return OpenApiUtils.createOpenAPI("Learning Service API", "1.0", "API for managing student's progress");
    }
}
