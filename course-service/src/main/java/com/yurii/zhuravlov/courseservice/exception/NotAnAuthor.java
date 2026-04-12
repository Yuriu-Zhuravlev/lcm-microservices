package com.yurii.zhuravlov.courseservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class NotAnAuthor extends CourseServiceException {
    public NotAnAuthor(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }

    public NotAnAuthor() {
        super("Access denied: You are not the author of this course", HttpStatus.FORBIDDEN);
    }
}
