package com.yurii.zhuravlov.courseservice.controller;

import com.yurii.zhuravlov.courseservice.config.annotation.CurrentUser;
import com.yurii.zhuravlov.courseservice.service.QuestionService;
import com.yurii.zhuravlov.requests.QuestionRequest;
import com.yurii.zhuravlov.responses.QuestionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping("/lesson/{lessonId}")
    public ResponseEntity<QuestionResponse> createQuestion(
            @PathVariable Long lessonId,
            @Valid @RequestBody QuestionRequest request,
            @CurrentUser Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(questionService.createQuestion(request, lessonId, userId));
    }

    @PutMapping("/{id}")
    public QuestionResponse updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody QuestionRequest request,
            @CurrentUser Long userId) {
        return questionService.updateQuestion(request, id, userId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable Long id,
            @CurrentUser Long userId) {
        questionService.deleteQuestion(id, userId);
        return ResponseEntity.noContent().build();
    }
}