package com.yurii.zhuravlov.authservice.service.userdetails;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CustomUserDetailsTest {

    @Test
    void constructor_ShouldAssignIdAndProperties() {
        Long expectedId = 5L;
        String username = "test";
        String password = "pass";
        Set<SimpleGrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("ROLE_USER"));

        CustomUserDetails details = new CustomUserDetails(username, password, authorities, expectedId);

        assertEquals(expectedId, details.getId());
        assertEquals(username, details.getUsername());
        assertEquals(password, details.getPassword());
        assertEquals(authorities, details.getAuthorities());
    }
}