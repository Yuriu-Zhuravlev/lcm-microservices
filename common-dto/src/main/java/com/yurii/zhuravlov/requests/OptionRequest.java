package com.yurii.zhuravlov.requests;

import jakarta.validation.constraints.NotBlank;

public record OptionRequest(
        @NotBlank String text,
        Boolean isCorrect
) {
    public OptionRequest {
        if (isCorrect == null) {
            isCorrect = false;
        }
    }
}
