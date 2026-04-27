package com.yurii.zhuravlov.requests;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record QuizSubmitRequest(
    @NotBlank Long lessonId,
    Map<Long, Character> answers
) {}