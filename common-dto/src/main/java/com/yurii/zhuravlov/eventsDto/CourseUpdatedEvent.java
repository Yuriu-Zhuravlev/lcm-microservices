package com.yurii.zhuravlov.eventsDto;

import com.yurii.zhuravlov.eventsDto.enums.CourseAction;
import lombok.Builder;

@Builder(toBuilder = true)
public record CourseUpdatedEvent(
        Long courseId,
        Long lessonId,
        int newTotalLessons,
        CourseAction action
) {}