package com.yurii.zhuravlov.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record QuestionRequest(
    @NotBlank String text,
    @Size(min = 2, message = "Question must have at least 2 options") List<OptionRequest> options
) {
}
