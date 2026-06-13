package com.yurii.zhuravlov.learningservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class CourseServiceUnavailableException extends LearningServiceException {
    public CourseServiceUnavailableException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE);
    }

    public CourseServiceUnavailableException() {
        super("Course service currently unavailable, please try again later", HttpStatus.SERVICE_UNAVAILABLE);
    }
}
