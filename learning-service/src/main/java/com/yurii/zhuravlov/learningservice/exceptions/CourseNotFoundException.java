package com.yurii.zhuravlov.learningservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CourseNotFoundException extends LearningServiceException {
    public CourseNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public CourseNotFoundException() {
        super("Course not found", HttpStatus.NOT_FOUND);
    }
}
