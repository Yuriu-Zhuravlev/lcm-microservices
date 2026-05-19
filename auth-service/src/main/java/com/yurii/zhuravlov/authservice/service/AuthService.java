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
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CACHE_KEY = "users::";

    public void register(RegistrationRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new UserAlreadyExists();
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(Set.of("ROLE_USER"));

        userRepository.save(user);
    }

    public String login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        if (user == null){
            throw new UserNotFound();
        }
        return jwtService.generateToken(user);
    }

    @Cacheable(value = "users", key = "#id")
    public UserResponse getUserById(Long id){
        User user = userRepository.findById(id).orElseThrow(UserNotFound::new);
        return new UserResponse(user.getId(), user.getUsername());
    }

    public Set<UserResponse> findUsersByIds(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Set.of();

        List<String> keys = userIds.stream()
                .map(id -> CACHE_KEY + id)
                .toList();

        List<Object> cachedUsers = redisTemplate.opsForValue().multiGet(keys);

        Set<UserResponse> result = new HashSet<>();
        Set<Long> missingIds = new HashSet<>();

        int i = 0;
        List<Long> idsList = userIds.stream().toList();
        for (Object cached : cachedUsers) {
            if (cached != null) {
                result.add((UserResponse) cached);
            } else {
                missingIds.add(idsList.get(i));
            }
            i++;
        }

        if (!missingIds.isEmpty()) {
            List<UserResponse> dbUsers = userRepository.findAllById(missingIds).stream()
                    .map(user -> new UserResponse(user.getId(), user.getUsername()))
                    .toList();

            Map<String, UserResponse> toCache = dbUsers.stream()
                    .collect(Collectors.toMap(u -> CACHE_KEY + u.id(), u -> u));

            redisTemplate.opsForValue().multiSet(toCache);
            toCache.keySet().forEach(key -> redisTemplate.expire(key, Duration.ofMinutes(60)));

            result.addAll(dbUsers);
        }

        return result;
    }
}