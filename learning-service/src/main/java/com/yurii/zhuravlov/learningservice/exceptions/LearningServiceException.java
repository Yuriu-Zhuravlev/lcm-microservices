package com.yurii.zhuravlov.learningservice.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class LearningServiceException extends RuntimeException {
    private final HttpStatus status;

    public LearningServiceException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

}
