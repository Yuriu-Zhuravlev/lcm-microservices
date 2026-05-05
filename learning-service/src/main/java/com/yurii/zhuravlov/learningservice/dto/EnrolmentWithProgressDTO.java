package com.yurii.zhuravlov.learningservice.dto;

import com.yurii.zhuravlov.learningservice.model.Enrolment;

public record EnrolmentWithProgressDTO(
        Enrolment enrolment,
        Long completedLessonsCount
) {}
