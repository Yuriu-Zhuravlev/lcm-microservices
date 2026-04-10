package com.yurii.zhuravlov.responses;

public record CourseResponse(Long id, String title, String description, UserResponse author) {
}
