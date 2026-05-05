package com.yurii.zhuravlov.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record EnrollmentResponse (
        Long id,
        CourseResponseShort course,
        String enrollmentStatus,
        LocalDateTime enrolledAt,
        LocalDateTime completedAt,
        Integer totalLessons,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer completedLessons,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<LessonProgressResponse> lessons
) {
}
