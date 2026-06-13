package com.yurii.zhuravlov.courseservice.client;

import com.yurii.zhuravlov.courseservice.client.fallback.AuthClientFallback;
import com.yurii.zhuravlov.responses.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Set;

@FeignClient(name = "auth-service", fallback = AuthClientFallback.class)
public interface AuthClient {

    @GetMapping("/api/auth/users/{id}")
    UserResponse getUserById(@PathVariable("id") Long id);

    @GetMapping("/api/auth/users")
    Set<UserResponse> getUsersByIds(@RequestParam Set<Long> ids);
}
