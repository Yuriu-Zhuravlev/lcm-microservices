package com.yurii.zhuravlov.authservice.service;

import com.yurii.zhuravlov.authservice.repository.UserRepository;
import com.yurii.zhuravlov.authservice.service.userdetails.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void loadUserByUsername_Success() {
        String username = "yurii";
        com.yurii.zhuravlov.authservice.entities.User entity = new com.yurii.zhuravlov.authservice.entities.User();
        entity.setId(10L);
        entity.setUsername(username);
        entity.setPassword("encoded_pass");
        entity.setRoles(Set.of("ROLE_USER", "ROLE_ADMIN"));

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(entity));

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        assertInstanceOf(CustomUserDetails.class, userDetails);
        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;

        assertEquals(10L, customUserDetails.getId());
        assertEquals(username, customUserDetails.getUsername());
        assertEquals("encoded_pass", customUserDetails.getPassword());

        var authorities = customUserDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertEquals(2, authorities.size());
        assertTrue(authorities.contains("ROLE_USER"));
        assertTrue(authorities.contains("ROLE_ADMIN"));
    }

    @Test
    void loadUserByUsername_UserNotFound_ThrowsException() {
        String username = "unknown";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () ->
                userDetailsService.loadUserByUsername(username));
    }
}