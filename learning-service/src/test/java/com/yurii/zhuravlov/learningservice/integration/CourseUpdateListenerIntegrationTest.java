package com.yurii.zhuravlov.learningservice.integration;

import com.yurii.zhuravlov.eventsDto.CourseUpdatedEvent;
import com.yurii.zhuravlov.eventsDto.enums.CourseAction;
import com.yurii.zhuravlov.learningservice.config.RabbitConfig;
import com.yurii.zhuravlov.learningservice.model.Enrolment;
import com.yurii.zhuravlov.learningservice.model.UserLessonProgress;
import com.yurii.zhuravlov.learningservice.model.enums.EnrolmentStatus;
import com.yurii.zhuravlov.learningservice.service.EnrollmentService;
import com.yurii.zhuravlov.responses.CourseResponseShort;
import com.yurii.zhuravlov.responses.ListEnrollmentResponses;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseUpdateListenerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private EnrollmentService enrollmentService;

    private CourseResponseShort mockCourse(Long courseId, int lessons) {
        return CourseResponseShort.builder()
                .id(courseId).title("Course").description("Desc")
                .totalLessonsCount(lessons).build();
    }

    private Enrolment saveEnrolment(Long userId, Long courseId, int totalLessons) {
        return enrolmentRepository.save(Enrolment.builder()
                .userId(userId).courseId(courseId)
                .status(EnrolmentStatus.ENROLLED)
                .enrolledAt(LocalDateTime.now())
                .totalLessonsCount(totalLessons).build());
    }

    private void publishEvent(CourseUpdatedEvent event) {
        rabbitTemplate.convertAndSend(RabbitConfig.COURSE_EXCHANGE, "", event);
    }

    @Test
    void handleAddLesson_ShouldUpdateTotalLessonsAndEvictCache() {
        Long userId = 1L;
        saveEnrolment(userId, 10L, 2);

        when(courseServiceClient.getAllCoursesByIds(List.of(10L)))
                .thenReturn(List.of(mockCourse(10L, 2)));

        enrollmentService.getEnrollmentsByUserId(userId);
        verify(courseServiceClient, times(1)).getAllCoursesByIds(List.of(10L));

        publishEvent(CourseUpdatedEvent.builder()
                .courseId(10L).newTotalLessons(3).action(CourseAction.ADD_LESSON).build());

        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Enrolment updated = enrolmentRepository.findAll().get(0);
            assertThat(updated.getTotalLessonsCount()).isEqualTo(3);
        });

        when(courseServiceClient.getAllCoursesByIds(List.of(10L)))
                .thenReturn(List.of(mockCourse(10L, 3)));
        enrollmentService.getEnrollmentsByUserId(userId);
        verify(courseServiceClient, times(2)).getAllCoursesByIds(List.of(10L));
    }

    @Test
    void handleRemoveLesson_ShouldDeleteProgressAndEvictCache() {
        Long userId = 1L;
        Enrolment enrolment = saveEnrolment(userId, 10L, 3);

        UserLessonProgress progress = userLessonProgressRepository.save(
                UserLessonProgress.builder()
                        .enrolment(enrolment).lessonId(99L)
                        .isCompleted(false).correctAnswers(0).totalQuestions(2).build());

        when(courseServiceClient.getAllCoursesByIds(List.of(10L)))
                .thenReturn(List.of(mockCourse(10L, 3)));
        enrollmentService.getEnrollmentsByUserId(userId);

        publishEvent(CourseUpdatedEvent.builder()
                .courseId(10L).lessonId(99L).newTotalLessons(2)
                .action(CourseAction.REMOVE_LESSON).build());

        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(userLessonProgressRepository.findById(progress.getId())).isEmpty();
            assertThat(enrolmentRepository.findAll().get(0).getTotalLessonsCount()).isEqualTo(2);
        });

        when(courseServiceClient.getAllCoursesByIds(List.of(10L)))
                .thenReturn(List.of(mockCourse(10L, 2)));
        enrollmentService.getEnrollmentsByUserId(userId);
        verify(courseServiceClient, times(2)).getAllCoursesByIds(List.of(10L));
    }

    @Test
    void handleRemoveCourse_ShouldDeleteEnrolmentsAndEvictCache() {
        Long userId = 1L;
        saveEnrolment(userId, 10L, 2);

        when(courseServiceClient.getAllCoursesByIds(List.of(10L)))
                .thenReturn(List.of(mockCourse(10L, 2)));
        enrollmentService.getEnrollmentsByUserId(userId);

        publishEvent(CourseUpdatedEvent.builder()
                .courseId(10L).action(CourseAction.REMOVE_COURSE).build());

        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(enrolmentRepository.findAll()).isEmpty()
        );

        ListEnrollmentResponses result = enrollmentService.getEnrollmentsByUserId(userId);
        assertThat(result.enrollmentResponses()).isEmpty();
    }

    @Test
    void handleUpdateLessonQuiz_ShouldDeleteProgressAndEvictCache() {
        Long userId = 1L;
        Enrolment enrolment = saveEnrolment(userId, 10L, 1);

        UserLessonProgress progress = userLessonProgressRepository.save(
                UserLessonProgress.builder()
                        .enrolment(enrolment).lessonId(55L)
                        .isCompleted(true).correctAnswers(3).totalQuestions(3).build());

        when(courseServiceClient.getAllCoursesByIds(List.of(10L)))
                .thenReturn(List.of(mockCourse(10L, 1)));
        enrollmentService.getEnrollmentsByUserId(userId);

        publishEvent(CourseUpdatedEvent.builder()
                .lessonId(55L).courseId(10L).action(CourseAction.UPDATE_LESSON_QUIZ).build());

        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(userLessonProgressRepository.findById(progress.getId())).isEmpty()
        );

        enrollmentService.getEnrollmentsByUserId(userId);
        verify(courseServiceClient, times(2)).getAllCoursesByIds(List.of(10L));
    }
}