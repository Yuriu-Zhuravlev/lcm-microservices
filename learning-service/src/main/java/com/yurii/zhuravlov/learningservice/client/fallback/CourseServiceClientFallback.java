package com.yurii.zhuravlov.learningservice.client.fallback;

import com.yurii.zhuravlov.learningservice.client.CourseServiceClient;
import com.yurii.zhuravlov.learningservice.exceptions.CourseServiceUnavailableException;
import com.yurii.zhuravlov.responses.CourseResponseFull;
import com.yurii.zhuravlov.responses.CourseResponseShort;
import com.yurii.zhuravlov.responses.LessonResponseFull;
import com.yurii.zhuravlov.responses.QuizCorrectAnswersResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class CourseServiceClientFallback implements CourseServiceClient {

    @Override
    public LessonResponseFull getLessonByIdInternal(Long id) {
        log.error("[CircuitBreaker] course-service unavailable: getLessonByIdInternal({})", id);
        throw new CourseServiceUnavailableException();
    }

    @Override
    public QuizCorrectAnswersResponse getCorrectAnswers(Long id) {
        log.error("[CircuitBreaker] course-service unavailable: getCorrectAnswers({})", id);
        throw new CourseServiceUnavailableException();
    }

    @Override
    public CourseResponseFull getCourseById(Long courseId) {
        log.error("[CircuitBreaker] course-service unavailable: getCourseById({})", courseId);
        throw new CourseServiceUnavailableException();
    }

    @Override
    public CourseResponseShort getCourseShortById(Long courseId) {
        log.error("[CircuitBreaker] course-service unavailable: getCourseShortById({})", courseId);
        throw new CourseServiceUnavailableException();
    }

    @Override
    public List<CourseResponseShort> getAllCoursesByIds(List<Long> ids) {
        log.warn("[CircuitBreaker] course-service unavailable: getAllCoursesByIds — return empty list");
        return Collections.emptyList();
    }
}