package com.yurii.zhuravlov.authservice.exceptions;

public class UserNotFound extends AuthServiceException {
    public UserNotFound(String message) {
        super(message);
    }
}
