package com.yurii.zhuravlov.errors;

public record ErrorResponse(
        String error,
        String message,
        String path,
        int status
) {}