package com.yurii.zhuravlov.authservice.controller;

import com.yurii.zhuravlov.authservice.entities.User;
import com.yurii.zhuravlov.authservice.service.AuthService;
import com.yurii.zhuravlov.requests.LoginRequest;
import com.yurii.zhuravlov.requests.RegistrationRequest;
import com.yurii.zhuravlov.responses.AuthResponse;
import com.yurii.zhuravlov.responses.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegistrationRequest request) {
        authService.register(request);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @GetMapping("/users/{id}")
    public UserResponse getUserById(@PathVariable("id") Long id) {
        User user = authService.getUserById(id);
        return new UserResponse(user.getId(), user.getUsername());
    }

    @GetMapping("/users")
    public Set<UserResponse> getUsersByIds(@RequestParam Set<Long> ids){
        return authService.findUsersByIds(ids);
    }
}
