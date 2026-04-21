package com.yurii.zhuravlov.responses;

import lombok.Builder;

import java.util.List;

@Builder
public record LessonResponseFull(
        Long id,
        String title,
        String htmlContent,
        int orderIndex,
        List<QuestionResponse> questions,
        Long courseId
) {
}
