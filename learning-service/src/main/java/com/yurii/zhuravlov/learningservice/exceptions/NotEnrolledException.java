package com.yurii.zhuravlov.learningservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class NotEnrolledException extends LearningServiceException {
    public NotEnrolledException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }

    public NotEnrolledException() {
        super("You are not enrolled to this course", HttpStatus.FORBIDDEN);
    }
}
