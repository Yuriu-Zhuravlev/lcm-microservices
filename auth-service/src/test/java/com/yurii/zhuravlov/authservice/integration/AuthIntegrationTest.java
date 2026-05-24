package com.yurii.zhuravlov.authservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yurii.zhuravlov.authservice.entities.User;
import com.yurii.zhuravlov.authservice.repository.UserRepository;
import com.yurii.zhuravlov.requests.LoginRequest;
import com.yurii.zhuravlov.requests.RegistrationRequest;
import com.yurii.zhuravlov.responses.AuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("cache-test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void register_ShouldReturnOk_WhenRequestIsValid() throws Exception {
        RegistrationRequest request = new RegistrationRequest("yurii_z", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));
    }

    @Test
    void register_ShouldReturnBadRequest_WhenUserAlreadyExists() throws Exception {
        RegistrationRequest request = new RegistrationRequest("yurii_z", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Auth service: exception occurred"));
    }

    @Test
    void register_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        RegistrationRequest request = new RegistrationRequest("", "123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void register_ThenLogin_ShouldReturnToken() throws Exception {
        RegistrationRequest request = new RegistrationRequest("yurii_z", "password123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        LoginRequest loginRequest = new LoginRequest("yurii_z", "password123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_ShouldReturnUnauthorized_WhenWrongPassword() throws Exception {
        RegistrationRequest request = new RegistrationRequest("yurii_z", "password123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        LoginRequest loginRequest = new LoginRequest("yurii_z", "wrong_password");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserById_ShouldReturnUser_WhenExistsInDb() throws Exception {
        registerAndExpectOk("yurii_z", "password123");
        String token = obtainToken("yurii_z", "password123");
        User user = userRepository.findByUsername("yurii_z").orElseThrow();

        mockMvc.perform(get("/api/auth/users/{id}", user.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("yurii_z"))
                .andExpect(jsonPath("$.id").value(user.getId()));
    }

    @Test
    void getUserById_ShouldReturn404_WhenNotExists() throws Exception {
        registerAndExpectOk("yurii_z", "password123");
        String token = obtainToken("yurii_z", "password123");

        mockMvc.perform(get("/api/auth/users/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserById_ShouldReturn403_WhenNoToken() throws Exception {
        mockMvc.perform(get("/api/auth/users/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUsersByIds_ShouldReturnUsers() throws Exception {
        registerAndExpectOk("yurii_z", "password123");
        String token = obtainToken("yurii_z", "password123");
        User user = userRepository.findByUsername("yurii_z").orElseThrow();

        mockMvc.perform(get("/api/auth/users")
                        .param("ids", user.getId().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("yurii_z"));
    }

    @Test
    void getUsersByIds_ShouldReturn403_WhenNoToken() throws Exception {
        mockMvc.perform(get("/api/auth/users")
                        .param("ids", "1"))
                .andExpect(status().isForbidden());
    }

    private void registerAndExpectOk(String username, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegistrationRequest(username, password))))
                .andExpect(status().isOk());
    }

    private String obtainToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(username, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class).token();
    }
}