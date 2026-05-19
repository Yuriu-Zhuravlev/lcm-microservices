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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
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
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void register_Success() {
        RegistrationRequest request = new RegistrationRequest("yurii", "password123");
        when(userRepository.findByUsername("yurii")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");

        authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("yurii", savedUser.getUsername());
        assertEquals("encoded_password", savedUser.getPassword());
        assertTrue(savedUser.getRoles().contains("ROLE_USER"));
    }

    @Test
    void register_ThrowsUserAlreadyExists() {
        RegistrationRequest request = new RegistrationRequest("existing_user", "pass");
        when(userRepository.findByUsername("existing_user")).thenReturn(Optional.of(new User()));

        assertThrows(UserAlreadyExists.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest("yurii", "password123");
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("generated_jwt_token");

        String token = authService.login(request);

        assertEquals("generated_jwt_token", token);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_ThrowsUserNotFound_WhenPrincipalIsNull() {
        LoginRequest request = new LoginRequest("hacker", "wrong_pass");
        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(null);

        assertThrows(UserNotFound.class, () -> authService.login(request));
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

    @Test
    void shouldReturnUsersFromCache() {
        Set<Long> ids = Set.of(1L, 2L);
        UserResponse user1 = new UserResponse(1L, "user1");
        UserResponse user2 = new UserResponse(2L, "user2");

        when(valueOperations.multiGet(anyCollection())).thenReturn(List.of(user1, user2));

        Set<UserResponse> result = authService.findUsersByIds(ids);

        assertThat(result).hasSize(2).containsExactlyInAnyOrder(user1, user2);
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldFetchFromDbWhenCacheIsPartial() {
        Set<Long> ids = Set.of(1L, 2L);
        UserResponse cachedUser = new UserResponse(1L, "user1");
        User dbUserEntity = new User(); dbUserEntity.setUsername("user2"); dbUserEntity.setId(2L);

        when(valueOperations.multiGet(anyCollection())).thenReturn(Arrays.asList(cachedUser, null));
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(dbUserEntity));

        Set<UserResponse> result = authService.findUsersByIds(ids);

        assertThat(result).hasSize(2);
        verify(valueOperations).multiSet(anyMap());
        verify(userRepository).findAllById(anyCollection());
    }
}