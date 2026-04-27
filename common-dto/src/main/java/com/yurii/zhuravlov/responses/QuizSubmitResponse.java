package com.yurii.zhuravlov.responses;

import lombok.Builder;

@Builder
public record QuizSubmitResponse(
    boolean isCompleted,
    int totalQuestions,
    int correctAnswers,
    double scorePercentage
) {}