package com.yurii.zhuravlov.learningservice.controller;

import com.yurii.zhuravlov.learningservice.config.annotation.CurrentUser;
import com.yurii.zhuravlov.learningservice.service.LessonService;
import com.yurii.zhuravlov.requests.QuizSubmitRequest;
import com.yurii.zhuravlov.responses.LessonResponseFull;
import com.yurii.zhuravlov.responses.QuizSubmitResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/learning/lesson")
@RequiredArgsConstructor
public class LessonController {
    private final LessonService lessonService;

    @GetMapping("/{lessonId}")
    public LessonResponseFull getLessonById(@PathVariable Long lessonId, @CurrentUser Long userId){
        return lessonService.getLessonContent(lessonId, userId);
    }

    @PostMapping("/{lessonId}/submit")
    public ResponseEntity<QuizSubmitResponse> submitQuiz(
            @PathVariable Long lessonId,
            @RequestBody Map<Long, Character> answers, // Приймаємо мапу прямо в запиті
            @CurrentUser Long userId) {

        QuizSubmitRequest request = new QuizSubmitRequest(lessonId, answers);
        return ResponseEntity.ok(lessonService.submitLessonProgress(userId, request));
    }
}
