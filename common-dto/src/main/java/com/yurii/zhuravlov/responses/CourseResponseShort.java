package com.yurii.zhuravlov.responses;

import lombok.Builder;

@Builder
public record CourseResponseShort(Long id, String title, String description, UserResponse author) {
}
