package com.yurii.zhuravlov.requests;

import jakarta.validation.constraints.NotNull;

public record CourseRequest(@NotNull String title, String description) {
}
