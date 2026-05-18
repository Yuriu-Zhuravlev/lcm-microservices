package com.yurii.zhuravlov.authservice.controller;

import com.yurii.zhuravlov.authservice.config.JwtAuthenticationFilter;
import com.yurii.zhuravlov.authservice.config.JwtService;
import com.yurii.zhuravlov.authservice.exceptions.UserAlreadyExists;
import com.yurii.zhuravlov.authservice.exceptions.UserNotFound;
import com.yurii.zhuravlov.authservice.handlers.GlobalExceptionHandler;
import com.yurii.zhuravlov.authservice.service.AuthService;
import com.yurii.zhuravlov.requests.LoginRequest;
import com.yurii.zhuravlov.requests.RegistrationRequest;
import com.yurii.zhuravlov.responses.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void register_ShouldReturnOk_WhenRequestIsValid() throws Exception {
        RegistrationRequest request = new RegistrationRequest("yurii_z", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));

        verify(authService).register(any(RegistrationRequest.class));
    }

    @Test
    void login_ShouldReturnToken_WhenCredentialsAreValid() throws Exception {
        LoginRequest request = new LoginRequest("yurii_z", "password123");
        String fakeToken = "fake-jwt-token";

        when(authService.login(any(LoginRequest.class))).thenReturn(fakeToken);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(fakeToken));
    }

    @Test
    void register_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        RegistrationRequest invalidRequest = new RegistrationRequest("", "123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").value(containsString("username: Username required")))
                .andExpect(jsonPath("$.message").value(containsString("password: Password must be between 6 and 20 characters")));
    }

    @Test
    void login_ShouldReturnUnauthorized_WhenBadCredentials() throws Exception {
        LoginRequest request = new LoginRequest("user", "wrong_pass");

        doThrow(new BadCredentialsException("Incorrect login or password"))
                .when(authService).login(any());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Bad Credentials"))
                .andExpect(jsonPath("$.message").value("Incorrect login or password"));
    }

    @Test
    void register_ShouldReturnBadRequest_WhenUserAlreadyExists() throws Exception {
        RegistrationRequest request = new RegistrationRequest("exists", "password123");

        doThrow(new UserAlreadyExists()).when(authService).register(any());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Auth service: exception occurred"));
    }

    @Test
    void getUserById_ShouldReturnUser_WhenExists() throws Exception {
        Long userId = 1L;
        UserResponse response = new UserResponse(userId, "yurii_z");

        when(authService.getUserById(userId)).thenReturn(response);

        mockMvc.perform(get("/api/auth/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.username").value("yurii_z"));
    }

    @Test
    void getUserById_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {
        Long userId = 999L;
        when(authService.getUserById(userId)).thenThrow(new UserNotFound());

        mockMvc.perform(get("/api/auth/users/{id}", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getUsersByIds_ShouldReturnSetOfUsers() throws Exception {
        Set<Long> ids = Set.of(1L, 2L);
        Set<UserResponse> expectedResponses = Set.of(
                new UserResponse(1L, "user1"),
                new UserResponse(2L, "user2")
        );

        when(authService.findUsersByIds(ids)).thenReturn(expectedResponses);

        mockMvc.perform(get("/api/auth/users")
                        .param("ids", "1", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.id == 1)].username").value("user1"))
                .andExpect(jsonPath("$[?(@.id == 2)].username").value("user2"));
    }

    @Test
    void getUsersByIds_ShouldReturnEmptySet_WhenNoIdsProvided() throws Exception {
        mockMvc.perform(get("/api/auth/users")
                        .param("ids", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}