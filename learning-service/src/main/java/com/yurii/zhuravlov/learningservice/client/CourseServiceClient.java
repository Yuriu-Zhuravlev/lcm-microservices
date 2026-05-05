package com.yurii.zhuravlov.learningservice.client;

import com.yurii.zhuravlov.responses.CourseResponseFull;
import com.yurii.zhuravlov.responses.CourseResponseShort;
import com.yurii.zhuravlov.responses.LessonResponseFull;
import com.yurii.zhuravlov.responses.QuizCorrectAnswersResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "course-service")
public interface CourseServiceClient {

    @GetMapping("/api/courses/lessons/internal/{id}")
    LessonResponseFull getLessonByIdInternal(@PathVariable Long id);

    @GetMapping("/api/courses/lessons/internal/answers/{id}")
    QuizCorrectAnswersResponse getCorrectAnswers(@PathVariable Long id);

    @GetMapping("/api/courses/{courseId}")
    CourseResponseFull getCourseById(@PathVariable Long courseId);

    @GetMapping("/api/courses/short/{courseId}")
    CourseResponseShort getCourseShortById(@PathVariable Long courseId);

    @GetMapping("/api/courses/byIds")
    List<CourseResponseShort> getAllCoursesByIds(@RequestParam List<Long> ids);
}
