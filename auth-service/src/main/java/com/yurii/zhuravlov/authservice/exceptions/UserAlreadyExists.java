package com.yurii.zhuravlov.authservice.exceptions;

public class UserAlreadyExists extends AuthServiceException {
    public UserAlreadyExists(String message) {
        super(message);
    }
}
