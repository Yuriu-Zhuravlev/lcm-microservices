package com.yurii.zhuravlov.courseservice.client.fallback;

import com.yurii.zhuravlov.courseservice.client.AuthClient;
import com.yurii.zhuravlov.responses.UserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
public class AuthClientFallback implements AuthClient {
    @Override
    public UserResponse getUserById(Long id) {
        log.warn("[CircuitBreaker] auth-service unavailable: getUserById({}), returning unknown user", id);
        return new UserResponse(id, "Unknown");
    }

    @Override
    public Set<UserResponse> getUsersByIds(Set<Long> ids) {
        log.warn("[CircuitBreaker] auth-service unavailable: getUsersByIds — return empty set");
        return Set.of();
    }
}
