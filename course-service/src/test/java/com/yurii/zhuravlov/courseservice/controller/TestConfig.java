package com.yurii.zhuravlov.courseservice.controller;

import com.yurii.zhuravlov.courseservice.config.annotation.CurrentUser;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@TestConfiguration
class TestConfig implements WebMvcConfigurer {
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(@NonNull MethodParameter parameter) {
                return parameter.hasParameterAnnotation(CurrentUser.class);
            }

            @Override
            public Object resolveArgument(@NonNull MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          @NonNull NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                return 1L;
            }
        });
    }
}