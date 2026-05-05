package com.yurii.zhuravlov.responses;

import lombok.Builder;

import java.util.Map;

@Builder
public record QuizCorrectAnswersResponse(
    Long lessonId,
    Long courseId,
    Map<Long, Character> answers
) {}