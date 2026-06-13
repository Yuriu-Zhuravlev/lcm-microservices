package com.yurii.zhuravlov.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;


@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/auth-service")
    public ResponseEntity<Map<String, Object>> authServiceFallback(HttpServletRequest request) {
        return buildFallbackResponse("auth-service", request.getRequestURI());
    }

    @RequestMapping("/course-service")
    public ResponseEntity<Map<String, Object>> courseServiceFallback(HttpServletRequest request) {
        return buildFallbackResponse("course-service", request.getRequestURI());
    }

    @RequestMapping("/learning-service")
    public ResponseEntity<Map<String, Object>> learningServiceFallback(HttpServletRequest request) {
        return buildFallbackResponse("learning-service", request.getRequestURI());
    }

    private ResponseEntity<Map<String, Object>> buildFallbackResponse(String service, String path) {
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "error", "Service Temporarily Unavailable",
                "message", "Service '" + service + "' temporarily unavailable. Try again later.",
                "path", path
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}