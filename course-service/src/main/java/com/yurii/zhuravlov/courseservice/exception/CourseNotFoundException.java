package com.yurii.zhuravlov.courseservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CourseNotFoundException extends CourseServiceException {
    public CourseNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public CourseNotFoundException() {
        super("Course was not found in our system", HttpStatus.NOT_FOUND);
    }
}
