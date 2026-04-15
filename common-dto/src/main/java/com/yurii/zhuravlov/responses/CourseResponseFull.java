package com.yurii.zhuravlov.responses;

import lombok.Builder;

import java.util.List;

@Builder
public record CourseResponseFull(Long id, String title, String description,
                                 UserResponse author, List<LessonResponseShort> lessons) {
}
