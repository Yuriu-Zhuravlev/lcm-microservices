package com.yurii.zhuravlov.courseservice.controller;

import com.yurii.zhuravlov.courseservice.annotation.CurrentUser;
import com.yurii.zhuravlov.courseservice.service.LessonService;
import com.yurii.zhuravlov.requests.LessonCreteRequest;
import com.yurii.zhuravlov.requests.LessonUpdateRequest;
import com.yurii.zhuravlov.responses.LessonResponseFull;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @PostMapping
    public ResponseEntity<LessonResponseFull> createLesson(
            @Valid @RequestBody LessonCreteRequest lessonCreteRequest,
            @CurrentUser Long userId){
        return ResponseEntity.status(HttpStatus.CREATED).body(
                lessonService.createLesson(lessonCreteRequest,userId));
    }

    @PutMapping("/{id}")
    public LessonResponseFull updateLesson(
            @PathVariable Long id,
            @Valid @RequestBody LessonUpdateRequest lessonUpdateRequest,
            @CurrentUser Long userId){
        return lessonService.updateLesson(id, lessonUpdateRequest, userId);
    }

    @GetMapping("/{id}")
    public LessonResponseFull getLessonById(@PathVariable Long id, @CurrentUser Long userId){
        return lessonService.getLessonById(id, userId);
    }

    @GetMapping("/internal/{id}")
    public LessonResponseFull getLessonByIdInternal(@PathVariable Long id,
                                                    @RequestHeader("X-Internal-Service") String internalService){
        if (!"learning-service".equals(internalService)) {
            throw new AccessDeniedException("Only internal services allowed");
        }
        return lessonService.getLessonByIdInternal(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long id, @CurrentUser Long userId) {
        lessonService.deleteLesson(id, userId);
        return ResponseEntity.noContent().build();
    }
}