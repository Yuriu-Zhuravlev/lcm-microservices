package com.yurii.zhuravlov.learningservice.handler;

import com.yurii.zhuravlov.errors.ErrorResponse;
import com.yurii.zhuravlov.learningservice.exceptions.LearningServiceException;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LearningServiceException.class)
    public ResponseEntity<ErrorResponse> handleLearningServiceException(LearningServiceException e, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                "Learning service: exception occurred",
                e.getMessage(),
                request.getRequestURI(),
                e.getStatus().value()
        );

        return new ResponseEntity<>(error, e.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponse error = new ErrorResponse(
                "Validation Failed",
                details,
                request.getRequestURI(),
                HttpStatus.BAD_REQUEST.value()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                "Internal Server Error",
                ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred",
                request.getRequestURI(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(FeignException e, HttpServletRequest request) {
        int status = e.status() != -1 ? e.status() : HttpStatus.INTERNAL_SERVER_ERROR.value();

        ErrorResponse error = new ErrorResponse(
                "External Service Error",
                "Error from Course or Auth service: " + e.getMessage(),
                request.getRequestURI(),
                status
        );

        return new ResponseEntity<>(error, HttpStatus.valueOf(status));
    }
}
