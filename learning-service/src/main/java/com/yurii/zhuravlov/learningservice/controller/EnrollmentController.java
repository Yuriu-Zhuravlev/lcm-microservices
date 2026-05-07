package com.yurii.zhuravlov.learningservice.controller;

import com.yurii.zhuravlov.learningservice.config.annotation.CurrentUser;
import com.yurii.zhuravlov.learningservice.service.EnrollmentService;
import com.yurii.zhuravlov.responses.EnrollmentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/learning/enrollment")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/{courseId}")
    public ResponseEntity<EnrollmentResponse> enroll(@PathVariable Long courseId, @CurrentUser Long userId){
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentService.enrollUser(userId, courseId));
    }

    @GetMapping("/my")
    public List<EnrollmentResponse> getMyEnrollments(@CurrentUser Long userId){
        return enrollmentService.getEnrollmentsByUserId(userId);
    }

    @GetMapping("/{id}")
    public EnrollmentResponse getEnrollmentById(@PathVariable Long id, @CurrentUser Long userId){
        return enrollmentService.getEnrollmentById(userId, id);
    }

    @PostMapping("/complete/{enrolmentId}")
    public ResponseEntity<Void> completeCourse(@PathVariable Long enrolmentId, @CurrentUser Long userId){
        enrollmentService.tryCompleteCourse(userId, enrolmentId);
        return ResponseEntity.noContent().build();
    }
}
