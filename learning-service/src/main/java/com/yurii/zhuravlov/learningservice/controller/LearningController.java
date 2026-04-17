package com.yurii.zhuravlov.learningservice.controller;

import com.yurii.zhuravlov.learningservice.client.AuthClient;
import com.yurii.zhuravlov.learningservice.client.CourseServiceClient;
import com.yurii.zhuravlov.learningservice.config.annotation.CurrentUser;
import com.yurii.zhuravlov.responses.LessonResponseFull;
import com.yurii.zhuravlov.responses.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class LearningController {
    private final AuthClient authClient;
    private final CourseServiceClient courseServiceClient;

    @GetMapping("/me")
    public UserResponse getMe(@CurrentUser Long userId){
        return authClient.getUserById(userId);
    }

    @GetMapping("/lesson")
    public LessonResponseFull getLesson(){
        return courseServiceClient.getLessonById(1L);
    }
}
