package com.yurii.zhuravlov.learningservice.controller;

import com.yurii.zhuravlov.learningservice.config.annotation.CurrentUser;
import com.yurii.zhuravlov.learningservice.service.LessonService;
import com.yurii.zhuravlov.responses.LessonResponseFull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learning/lesson")
@RequiredArgsConstructor
public class LessonController {
    private final LessonService lessonService;

    @GetMapping("/{lessonId}")
    public LessonResponseFull getLessonById(@PathVariable Long lessonId, @CurrentUser Long userId){
        return lessonService.getLessonContent(lessonId, userId);
    }
}
