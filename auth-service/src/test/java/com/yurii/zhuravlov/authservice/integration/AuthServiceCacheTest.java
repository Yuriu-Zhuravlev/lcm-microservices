package com.yurii.zhuravlov.authservice.integration;

import com.yurii.zhuravlov.authservice.entities.User;
import com.yurii.zhuravlov.authservice.service.AuthService;
import com.yurii.zhuravlov.requests.LoginRequest;
import com.yurii.zhuravlov.responses.UserResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class AuthServiceCacheTest extends BaseIntegrationTest{

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Test
    public void getUserById_ShouldCacheResult_OnSecondCall() {
        User user = new User();
        user.setUsername("cached_user");
        user.setPassword("encoded");
        user.setRoles(Set.of("ROLE_USER"));
        userRepository.save(user);

        UserResponse first = authService.getUserById(user.getId());
        assertThat(first.username()).isEqualTo("cached_user");

        Object cached = redisTemplate.opsForValue().get("users::" + user.getId());
        assertThat(cached).isNotNull();
        assertThat(((UserResponse) cached).username()).isEqualTo("cached_user");

        userRepository.deleteAll();
        UserResponse second = authService.getUserById(user.getId());
        assertThat(second.username()).isEqualTo("cached_user");
    }

    @Test
    public void findUsersByIds_ShouldCacheAndReturnFromCache() {
        User u1 = new User(); u1.setUsername("u1"); u1.setPassword("p"); u1.setRoles(Set.of("ROLE_USER"));
        User u2 = new User(); u2.setUsername("u2"); u2.setPassword("p"); u2.setRoles(Set.of("ROLE_USER"));
        userRepository.saveAll(List.of(u1, u2));

        Set<Long> ids = Set.of(u1.getId(), u2.getId());

        Set<UserResponse> first = authService.findUsersByIds(ids);
        assertThat(first).hasSize(2);

        userRepository.deleteAll();
        Set<UserResponse> second = authService.findUsersByIds(ids);
        assertThat(second).hasSize(2);
        assertThat(second.stream().map(UserResponse::username).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder("u1", "u2");
    }

    @Test
    public void login_shouldCache(){
        User u1 = new User(); u1.setUsername("u1"); u1.setPassword(passwordEncoder.encode("p1")); u1.setRoles(Set.of("ROLE_USER"));
        userRepository.save(u1);
        authService.login(new LoginRequest("u1","p1"));
        Object cached = redisTemplate.opsForValue().get("userDetails::u1");
        assertThat(cached).isNotNull();
    }
}