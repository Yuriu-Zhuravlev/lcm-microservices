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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public void register(RegistrationRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new UserAlreadyExists("User already exists");
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
            throw new UserNotFound("User not found");
        }
        return jwtService.generateToken(user);
    }

    public User getUserById(Long id){
        return userRepository.findById(id).orElseThrow(() -> new UserNotFound("User not found"));
    }

    public Set<UserResponse> findUsersByIds(Set<Long> userIds){
        return userRepository.findAllById(userIds).stream()
                .map(user -> new UserResponse(user.getId(), user.getUsername()))
                .collect(Collectors.toSet());
    }
}