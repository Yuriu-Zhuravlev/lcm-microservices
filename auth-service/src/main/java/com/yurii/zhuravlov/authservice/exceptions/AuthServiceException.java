package com.yurii.zhuravlov.authservice.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AuthServiceException extends RuntimeException {
    private final HttpStatus status;

    public AuthServiceException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

}
