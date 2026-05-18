package com.yurii.zhuravlov.authservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UserAlreadyExists extends AuthServiceException {
    public UserAlreadyExists(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    public UserAlreadyExists() {
        super("User already exists", HttpStatus.BAD_REQUEST);
    }
}
