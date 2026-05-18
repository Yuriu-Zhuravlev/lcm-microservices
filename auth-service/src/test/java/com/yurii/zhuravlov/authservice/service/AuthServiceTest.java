package com.yurii.zhuravlov.authservice.service;

import com.yurii.zhuravlov.authservice.config.JwtService;
import com.yurii.zhuravlov.authservice.entities.User;
import com.yurii.zhuravlov.authservice.exceptions.UserAlreadyExists;
import com.yurii.zhuravlov.authservice.exceptions.UserNotFound;
import com.yurii.zhuravlov.authservice.repository.UserRepository;
import com.yurii.zhuravlov.authservice.service.userdetails.CustomUserDetails;
import com.yurii.zhuravlov.requests.LoginRequest;
import com.yurii.zhuravlov.requests.RegistrationRequest;
import com.yurii.zhuravlov.responses.UserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_Success() {
        // Given
        RegistrationRequest request = new RegistrationRequest("yurii", "password123");
        when(userRepository.findByUsername("yurii")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");

        // When
        authService.register(request);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("yurii", savedUser.getUsername());
        assertEquals("encoded_password", savedUser.getPassword());
        assertTrue(savedUser.getRoles().contains("ROLE_USER"));
    }

    @Test
    void register_ThrowsUserAlreadyExists() {
        // Given
        RegistrationRequest request = new RegistrationRequest("existing_user", "pass");
        when(userRepository.findByUsername("existing_user")).thenReturn(Optional.of(new User()));

        // When & Then
        assertThrows(UserAlreadyExists.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_Success() {
        // Given
        LoginRequest request = new LoginRequest("yurii", "password123");
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("generated_jwt_token");

        // When
        String token = authService.login(request);

        // Then
        assertEquals("generated_jwt_token", token);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_ThrowsUserNotFound_WhenPrincipalIsNull() {
        // Given
        LoginRequest request = new LoginRequest("hacker", "wrong_pass");
        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(null);

        // When & Then
        assertThrows(UserNotFound.class, () -> authService.login(request));
    }

    @Test
    void findUsersByIds_ReturnsMappedResponses() {
        // Given
        Set<Long> ids = Set.of(1L, 2L);
        User user1 = new User(); user1.setId(1L); user1.setUsername("user1");
        User user2 = new User(); user2.setId(2L); user2.setUsername("user2");

        when(userRepository.findAllById(ids)).thenReturn(List.of(user1, user2));

        // When
        Set<UserResponse> responses = authService.findUsersByIds(ids);

        // Then
        assertEquals(2, responses.size());
        assertTrue(responses.stream().anyMatch(r -> r.username().equals("user1")));
        assertTrue(responses.stream().anyMatch(r -> r.id().equals(2L)));
    }

    @Test
    void getUserById_ReturnsMappedResponse(){
        User user = new User(); user.setId(1L); user.setUsername("user");

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        UserResponse response = authService.getUserById(1L);

        assertEquals("user", response.username());
        assertEquals(1L, response.id());
    }

    @Test
    void getUserById_NotFound(){
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());
        assertThrows(UserNotFound.class,() -> authService.getUserById(1L));
    }
}