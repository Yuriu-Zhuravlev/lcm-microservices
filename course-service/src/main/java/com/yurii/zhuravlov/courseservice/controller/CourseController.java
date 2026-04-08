package com.yurii.zhuravlov.courseservice.controller;

import com.yurii.zhuravlov.courseservice.model.Course;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    @GetMapping("/all")
    public List<Course> getAllCourses(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        // Logging to verify that Gateway successfully extracted User ID from JWT
        System.out.println("Request from User ID: " + userId);

        return List.of(
                new Course(1L, "Microservices with Spring Boot", "Yurii"),
                new Course(2L, "Advanced Java 25", "Yurii")
        );
    }
}