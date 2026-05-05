package com.yurii.zhuravlov.learningservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class EnrollmentNotFoundException extends LearningServiceException {
    public EnrollmentNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public EnrollmentNotFoundException() {
        super("Enrollment not found", HttpStatus.NOT_FOUND);
    }
}
