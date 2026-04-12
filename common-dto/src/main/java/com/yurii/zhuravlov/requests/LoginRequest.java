package com.yurii.zhuravlov.requests;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Username required")
        String username,
        @NotBlank(message = "Password required")
        String password
) {
}
