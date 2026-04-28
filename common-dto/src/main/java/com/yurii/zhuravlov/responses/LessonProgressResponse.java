package com.yurii.zhuravlov.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record LessonProgressResponse(
    Long id,
    String title,
    Integer correctAnswers,
    Integer totalQuestions,
    boolean isCompleted,
    @JsonInclude(JsonInclude.Include.NON_NULL) LocalDateTime completedAt
) {}