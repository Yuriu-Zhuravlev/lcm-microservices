package com.yurii.zhuravlov.learningservice.service;

import com.yurii.zhuravlov.learningservice.client.CourseServiceClient;
import com.yurii.zhuravlov.learningservice.exceptions.NotEnrolledException;
import com.yurii.zhuravlov.learningservice.model.Enrolment;
import com.yurii.zhuravlov.learningservice.model.UserLessonProgress;
import com.yurii.zhuravlov.learningservice.repo.EnrolmentRepository;
import com.yurii.zhuravlov.learningservice.repo.UserLessonProgressRepository;
import com.yurii.zhuravlov.requests.QuizSubmitRequest;
import com.yurii.zhuravlov.responses.LessonResponseFull;
import com.yurii.zhuravlov.responses.QuizCorrectAnswersResponse;
import com.yurii.zhuravlov.responses.QuizSubmitResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LessonService {
    private final EnrolmentRepository enrolmentRepository;
    private final CourseServiceClient courseServiceClient;
    private final UserLessonProgressRepository userLessonProgressRepository;
    private final RedisTemplate<String, Object> redisTemplate;


    @Transactional
    public LessonResponseFull getLessonContent(Long lessonId, Long userId) {
        LessonResponseFull lesson = courseServiceClient.getLessonByIdInternal(lessonId);

        if (!enrolmentRepository.existsByUserIdAndCourseId(userId, lesson.courseId())) {
            throw new NotEnrolledException();
        }

        return lesson;
    }

    @Transactional
    public QuizSubmitResponse submitLessonProgress(Long userId, QuizSubmitRequest request) {
        QuizCorrectAnswersResponse correctAnswers = courseServiceClient.getCorrectAnswers(request.lessonId());

        Map<Long, Character> studentAnswers = request.answers();
        Map<Long, Character> actualAnswers = correctAnswers.answers();

        boolean isCompleted;
        int totalQuestions = 0;
        long correctCount = 0;

        if (actualAnswers.isEmpty()) {
            isCompleted = true;
        } else {
            totalQuestions = actualAnswers.size();
            correctCount = actualAnswers.entrySet().stream()
                    .filter(entry -> Character.toUpperCase(entry.getValue())
                            == Character.toUpperCase(studentAnswers.get(entry.getKey())))
                    .count();

            isCompleted = (double) correctCount / totalQuestions >= 0.8;
        }

        Enrolment enrolment = enrolmentRepository.findByUserIdAndCourseId(userId, correctAnswers.courseId())
                .orElseThrow(NotEnrolledException::new);

        UserLessonProgress progress = userLessonProgressRepository
                .findByEnrolmentIdAndLessonId(enrolment.getId(), request.lessonId())
                .orElse(UserLessonProgress.builder()
                        .enrolment(enrolment)
                        .lessonId(request.lessonId())
                        .isCompleted(false)
                        .correctAnswers(0)
                        .totalQuestions(totalQuestions)
                        .build());

        boolean wasCompleted = progress.getIsCompleted();

        if (!progress.getIsCompleted() && isCompleted) {
            progress.setIsCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
        }

        if (correctCount > progress.getCorrectAnswers()) {
            progress.setCorrectAnswers((int) correctCount);
            progress.setTotalQuestions(totalQuestions);
        }

        userLessonProgressRepository.save(progress);

        if (!wasCompleted && isCompleted) {
            redisTemplate.delete("user-enrollments::" + userId);
        }

        double rawPercentage = totalQuestions == 0 ? 100.0 : ((double) correctCount / totalQuestions) * 100;
        double roundedPercentage = BigDecimal.valueOf(rawPercentage)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        return QuizSubmitResponse.builder()
                .isCompleted(isCompleted)
                .totalQuestions(totalQuestions)
                .correctAnswers((int) correctCount)
                .scorePercentage(roundedPercentage)
                .build();
    }

}
