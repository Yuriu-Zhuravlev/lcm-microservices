package com.yurii.zhuravlov.courseservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class LessonNotFoundException extends CourseServiceException {
    public LessonNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public LessonNotFoundException() {
        super("Lesson was not found in our system", HttpStatus.NOT_FOUND);
    }
}
