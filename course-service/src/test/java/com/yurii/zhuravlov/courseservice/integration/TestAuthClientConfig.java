package com.yurii.zhuravlov.courseservice.integration;

import com.yurii.zhuravlov.courseservice.client.AuthClient;
import com.yurii.zhuravlov.responses.UserResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Set;
import java.util.stream.Collectors;

@TestConfiguration
public class TestAuthClientConfig {

    @Bean
    @Primary
    public AuthClient authClient() {
        return new AuthClient() {
            @Override
            public UserResponse getUserById(Long id) {
                return new UserResponse(id, "TestUser_" + id);
            }

            @Override
            public Set<UserResponse> getUsersByIds(Set<Long> ids) {
                return ids.stream()
                        .map(id -> new UserResponse(id, "TestUser_" + id))
                        .collect(Collectors.toSet());
            }
        };
    }
}