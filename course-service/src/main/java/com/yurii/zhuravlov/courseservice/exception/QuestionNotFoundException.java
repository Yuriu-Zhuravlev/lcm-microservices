package com.yurii.zhuravlov.courseservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class QuestionNotFoundException extends CourseServiceException {
    public QuestionNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public QuestionNotFoundException() {
        super("Question was not found in our system", HttpStatus.NOT_FOUND);
    }
}
