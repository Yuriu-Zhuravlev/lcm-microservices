package com.yurii.zhuravlov.learningservice.service;

import com.yurii.zhuravlov.learningservice.client.CourseServiceClient;
import com.yurii.zhuravlov.learningservice.dto.EnrolmentWithProgressDTO;
import com.yurii.zhuravlov.learningservice.exceptions.*;
import com.yurii.zhuravlov.learningservice.model.Enrolment;
import com.yurii.zhuravlov.learningservice.model.UserLessonProgress;
import com.yurii.zhuravlov.learningservice.model.enums.EnrolmentStatus;
import com.yurii.zhuravlov.learningservice.repo.EnrolmentRepository;
import com.yurii.zhuravlov.responses.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentService {
    private final EnrolmentRepository enrolmentRepository;
    private final CourseServiceClient courseServiceClient;

    @Transactional
    @CacheEvict(value = "user-enrollments", key = "#userId")
    public EnrollmentResponse enrollUser(Long userId, Long courseId) {
        if (enrolmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new AlreadyEnrolledException();
        }
        CourseResponseShort courseResponseShort = courseServiceClient.getCourseShortById(courseId);
        Enrolment enrolment = Enrolment.builder()
                .userId(userId)
                .courseId(courseId)
                .status(EnrolmentStatus.ENROLLED)
                .enrolledAt(LocalDateTime.now())
                .totalLessonsCount(courseResponseShort.totalLessonsCount())
                .build();

        enrolment = enrolmentRepository.save(enrolment);

        return getEnrollmentResponse(enrolment, courseResponseShort, 0);
    }

    @Transactional
    public EnrollmentResponse getEnrollmentById(Long userId, Long enrollmentId) {
        Enrolment enrolment = enrolmentRepository.findById(enrollmentId).orElseThrow(EnrollmentNotFoundException::new);
        if (!enrolment.getUserId().equals(userId)) {
            throw new NotEnrolledException("Not your enrolment");
        }
        CourseResponseFull course = courseServiceClient.getCourseById(enrolment.getCourseId());
        List<LessonProgressResponse> lessonProgress = null;
        if (course.lessons() != null) {
            List<UserLessonProgress> progressList = enrolment.getLessonsProgress();
            Map<Long, UserLessonProgress> progressMap;
            if (progressList != null) {
                progressMap = progressList.stream()
                        .collect(Collectors.toMap(UserLessonProgress::getLessonId, p -> p));
            } else {
                progressMap = new HashMap<>();
            }
            lessonProgress = course.lessons().stream()
                    .map(lesson -> {
                        UserLessonProgress p = progressMap.get(lesson.id());
                        return LessonProgressResponse.builder()
                                .id(lesson.id())
                                .title(lesson.title())
                                .correctAnswers(p != null ? p.getCorrectAnswers() : 0)
                                .totalQuestions(p != null ? p.getTotalQuestions() : 0)
                                .isCompleted(p != null && p.getIsCompleted())
                                .completedAt(p != null ? p.getCompletedAt() : null)
                                .build();
                    })
                    .toList();
        }
        CourseResponseShort courseResponseShort = CourseResponseShort.builder()
                .id(course.id())
                .title(course.title())
                .description(course.description())
                .author(course.author())
                .totalLessonsCount(lessonProgress == null ? 0: lessonProgress.size())
                .build();
        return getEnrollmentResponse(enrolment,courseResponseShort, lessonProgress);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "user-enrollments", key = "#userId")
    public ListEnrollmentResponses getEnrollmentsByUserId(Long userId) {
        List<EnrolmentWithProgressDTO> results = enrolmentRepository.findByUserIdWithCompletedCount(userId);

        if (results.isEmpty()) return new ListEnrollmentResponses(Collections.emptyList());

        List<CourseResponseShort> courses = courseServiceClient.getAllCoursesByIds(
                results.stream()
                        .map(r -> r.enrolment().getCourseId())
                        .toList()
        );
        Map<Long, CourseResponseShort> courseMap = courses.stream()
                .collect(Collectors.toMap(CourseResponseShort::id, Function.identity()));

        return new ListEnrollmentResponses(results.stream().map(res -> {
            CourseResponseShort courseResponseShort = courseMap.get(res.enrolment().getCourseId());
            if (courseResponseShort == null) {
                throw new CourseNotFoundException("Course with id = " + res.enrolment().getCourseId() + " not found");
            }
            return getEnrollmentResponse(res.enrolment(), courseResponseShort, res.completedLessonsCount().intValue());
        }).toList());
    }

    @Transactional
    @CacheEvict(value = "user-enrollments", key = "#userId")
    public void tryCompleteCourse(Long userId, Long enrollmentId) {
        Enrolment enrolment = enrolmentRepository.findById(enrollmentId)
                .orElseThrow(EnrollmentNotFoundException::new);

        if (!enrolment.getUserId().equals(userId)) {
            throw new NotEnrolledException("Not your enrolment");
        }

        long completedLessonsCount = enrolmentRepository.countCompletedLessons(enrollmentId);

        if (enrolment.getStatus() == EnrolmentStatus.COMPLETED) {
            return;
        }

        if (completedLessonsCount == enrolment.getTotalLessonsCount()) {
            enrolment.setStatus(EnrolmentStatus.COMPLETED);
            enrolment.setCompletedAt(LocalDateTime.now());
            enrolmentRepository.save(enrolment);
        } else {
            throw new CourseNotCompletedException("Course not completed: you completed only " + completedLessonsCount +
                    " of " + enrolment.getTotalLessonsCount() + " lessons.");
        }
    }

    private static EnrollmentResponse getEnrollmentResponse(Enrolment enrolment,
                                                            CourseResponseShort courseResponseShort,
                                                            Integer completedLessons) {
        return EnrollmentResponse.builder()
                .id(enrolment.getId())
                .course(courseResponseShort)
                .enrollmentStatus(enrolment.getStatus().toString())
                .enrolledAt(enrolment.getEnrolledAt())
                .completedAt(enrolment.getCompletedAt())
                .totalLessons(enrolment.getTotalLessonsCount())
                .completedLessons(completedLessons)
                .build();
    }

    private static EnrollmentResponse getEnrollmentResponse(Enrolment enrolment,
                                                            CourseResponseShort courseResponseShort,
                                                            List<LessonProgressResponse> lessonProgress) {
        int completedLessons = Math.toIntExact(lessonProgress == null ? 0 :
                lessonProgress.stream().filter(LessonProgressResponse::isCompleted).count());
        return EnrollmentResponse.builder()
                .id(enrolment.getId())
                .course(courseResponseShort)
                .enrollmentStatus(enrolment.getStatus().toString())
                .enrolledAt(enrolment.getEnrolledAt())
                .completedAt(enrolment.getCompletedAt())
                .totalLessons(enrolment.getTotalLessonsCount())
                .completedLessons(completedLessons)
                .lessons(lessonProgress)
                .build();

    }
}
