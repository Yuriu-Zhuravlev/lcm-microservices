package com.yurii.zhuravlov.authservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFound extends AuthServiceException {
    public UserNotFound(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public UserNotFound() {
        super("User not found", HttpStatus.NOT_FOUND);
    }
}
