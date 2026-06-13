package com.yurii.zhuravlov.learningservice.handler;

import com.yurii.zhuravlov.errors.ErrorResponse;
import com.yurii.zhuravlov.learningservice.exceptions.LearningServiceException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ErrorResponse> handleCallNotPermitted(CallNotPermittedException e, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                "Service Temporarily Unavailable",
                "Circuit Breaker if open for '" + e.getCausingCircuitBreakerName()
                        + "'. Service temporary unavailable.",
                request.getRequestURI(),
                HttpStatus.SERVICE_UNAVAILABLE.value()
        );
        return new ResponseEntity<>(error, HttpStatus.SERVICE_UNAVAILABLE);
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
