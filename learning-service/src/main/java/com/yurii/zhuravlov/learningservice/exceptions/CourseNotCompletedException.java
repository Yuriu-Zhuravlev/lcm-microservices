package com.yurii.zhuravlov.learningservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CourseNotCompletedException extends LearningServiceException {
    public CourseNotCompletedException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    public CourseNotCompletedException() {
        super("You did not complete the course", HttpStatus.BAD_REQUEST);
    }
}
