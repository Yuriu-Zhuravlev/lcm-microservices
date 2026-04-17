package com.yurii.zhuravlov.responses;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record EnrollmentResponse (
        Long id,
        CourseResponseShort course,
        String enrollmentStatus,
        LocalDateTime enrolledAt
) {
}
