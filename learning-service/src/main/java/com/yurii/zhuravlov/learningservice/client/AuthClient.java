package com.yurii.zhuravlov.learningservice.client;

import com.yurii.zhuravlov.responses.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service")
public interface AuthClient {

    @GetMapping("/api/auth/users/{id}")
    UserResponse getUserById(@PathVariable("id") Long id);
}
