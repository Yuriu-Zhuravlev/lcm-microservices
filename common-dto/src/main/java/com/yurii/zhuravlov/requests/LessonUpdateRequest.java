package com.yurii.zhuravlov.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LessonUpdateRequest(
        @NotBlank String title,
        @NotBlank String htmlContent,
        @NotNull int orderIndex
) {
}
