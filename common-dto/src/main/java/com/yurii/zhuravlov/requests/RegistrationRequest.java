package com.yurii.zhuravlov.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
        @NotBlank String username,
        @NotBlank @Size(min = 6) String password
) {
}
