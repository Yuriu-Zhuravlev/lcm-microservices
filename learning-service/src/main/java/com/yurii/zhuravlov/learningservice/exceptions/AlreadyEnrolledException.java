package com.yurii.zhuravlov.learningservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class AlreadyEnrolledException extends LearningServiceException {
    public AlreadyEnrolledException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    public AlreadyEnrolledException() {
        super("You already enrolled to this course", HttpStatus.BAD_REQUEST);
    }
}
