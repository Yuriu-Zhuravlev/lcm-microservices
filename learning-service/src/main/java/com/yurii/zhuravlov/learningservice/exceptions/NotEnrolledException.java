package com.yurii.zhuravlov.learningservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class NotEnrolledException extends LearningServiceException {
    public NotEnrolledException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    public NotEnrolledException() {
        super("You are not enrolled to this course", HttpStatus.BAD_REQUEST);
    }
}
