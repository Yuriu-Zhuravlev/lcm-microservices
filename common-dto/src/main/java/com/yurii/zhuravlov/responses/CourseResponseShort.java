package com.yurii.zhuravlov.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
public record CourseResponseShort(
        Long id,
        String title,
        String description,
        UserResponse author,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer totalLessonsCount
) {
}
