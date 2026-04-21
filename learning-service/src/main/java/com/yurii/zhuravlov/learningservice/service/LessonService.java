package com.yurii.zhuravlov.learningservice.service;

import com.yurii.zhuravlov.learningservice.client.CourseServiceClient;
import com.yurii.zhuravlov.learningservice.exceptions.NotEnrolledException;
import com.yurii.zhuravlov.learningservice.repo.EnrolmentRepository;
import com.yurii.zhuravlov.responses.LessonResponseFull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LessonService {
    private final EnrolmentRepository enrolmentRepository;
    private final CourseServiceClient courseServiceClient;

    @Transactional
    public LessonResponseFull getLessonContent(Long lessonId, Long userId) {
        LessonResponseFull lesson = courseServiceClient.getLessonByIdInternal(lessonId);

        if (!enrolmentRepository.existsByUserIdAndCourseId(userId, lesson.courseId())) {
            throw new NotEnrolledException();
        }

        return lesson;
    }
}
