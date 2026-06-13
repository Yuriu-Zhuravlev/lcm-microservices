package com.yurii.zhuravlov.learningservice.integration;

import com.yurii.zhuravlov.learningservice.exceptions.AlreadyEnrolledException;
import com.yurii.zhuravlov.learningservice.model.Enrolment;
import com.yurii.zhuravlov.learningservice.model.enums.EnrolmentStatus;
import com.yurii.zhuravlov.learningservice.service.EnrollmentService;
import com.yurii.zhuravlov.learningservice.service.LessonService;
import com.yurii.zhuravlov.requests.QuizSubmitRequest;
import com.yurii.zhuravlov.responses.CourseResponseShort;
import com.yurii.zhuravlov.responses.ListEnrollmentResponses;
import com.yurii.zhuravlov.responses.QuizCorrectAnswersResponse;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@TestMethodOrder(MethodOrderer.Random.class)
class EnrollmentServiceCacheTest extends BaseIntegrationTest {

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private LessonService lessonService;

    private CourseResponseShort mockCourse(Long courseId) {
        return CourseResponseShort.builder()
                .id(courseId).title("Course " + courseId)
                .description("Desc").totalLessonsCount(3).build();
    }

    private void saveEnrolment(Long userId, Long courseId) {
        enrolmentRepository.save(Enrolment.builder()
                .userId(userId).courseId(courseId)
                .status(EnrolmentStatus.ENROLLED)
                .enrolledAt(LocalDateTime.now())
                .totalLessonsCount(3).build());
    }

    @Test
    void getEnrollmentsByUserId_ShouldCacheResult() {
        Long userId = 1L;
        saveEnrolment(userId, 10L);

        when(courseServiceClient.getAllCoursesByIds(List.of(10L)))
                .thenReturn(List.of(mockCourse(10L)));

        ListEnrollmentResponses first = enrollmentService.getEnrollmentsByUserId(userId);
        assertThat(first.enrollmentResponses()).hasSize(1);

        ListEnrollmentResponses second = enrollmentService.getEnrollmentsByUserId(userId);
        assertThat(second.enrollmentResponses()).hasSize(1);

        verify(courseServiceClient, times(1)).getAllCoursesByIds(List.of(10L));
    }

    @Test
    void enrollUser_ShouldEvictCache() {
        Long userId = 1L;
        saveEnrolment(userId, 10L);

        when(courseServiceClient.getAllCoursesByIds(List.of(10L)))
                .thenReturn(List.of(mockCourse(10L)));
        when(courseServiceClient.getCourseShortById(20L))
                .thenReturn(mockCourse(20L));

        enrollmentService.getEnrollmentsByUserId(userId);
        verify(courseServiceClient, times(1)).getAllCoursesByIds(List.of(10L));

        enrollmentService.enrollUser(userId, 20L);

        when(courseServiceClient.getAllCoursesByIds(List.of(10L, 20L)))
                .thenReturn(List.of(mockCourse(10L), mockCourse(20L)));
        ListEnrollmentResponses result = enrollmentService.getEnrollmentsByUserId(userId);
        assertThat(result.enrollmentResponses()).hasSize(2);
    }

    @Test
    void enrollUser_AlreadyEnrolled_ShouldThrow() {
        Long userId = 1L;
        saveEnrolment(userId, 10L);
        when(courseServiceClient.getCourseShortById(10L)).thenReturn(mockCourse(10L));

        assertThatThrownBy(() -> enrollmentService.enrollUser(userId, 10L))
                .isInstanceOf(AlreadyEnrolledException.class);
    }

    @Test
    void tryCompleteCourse_ShouldEvictCache() {
        Long userId = 1L;
        Enrolment enrolment = enrolmentRepository.save(Enrolment.builder()
                .userId(userId).courseId(10L)
                .status(EnrolmentStatus.ENROLLED)
                .enrolledAt(LocalDateTime.now())
                .totalLessonsCount(0).build());

        when(courseServiceClient.getAllCoursesByIds(List.of(10L)))
                .thenReturn(List.of(mockCourse(10L)));

        enrollmentService.getEnrollmentsByUserId(userId);
        verify(courseServiceClient, times(1)).getAllCoursesByIds(List.of(10L));

        enrollmentService.tryCompleteCourse(userId, enrolment.getId());

        enrollmentService.getEnrollmentsByUserId(userId);
        verify(courseServiceClient, times(2)).getAllCoursesByIds(List.of(10L));
    }

    @Test
    void trySubmitProgress_shouldEvictCache(){
        Long userId = 1L;
        saveEnrolment(userId, 10L);

        when(courseServiceClient.getAllCoursesByIds(List.of(10L)))
                .thenReturn(List.of(mockCourse(10L)));

        enrollmentService.getEnrollmentsByUserId(userId);
        verify(courseServiceClient, times(1)).getAllCoursesByIds(List.of(10L));

        Map<Long, Character> answers = new HashMap<>(Map.ofEntries(
                entry(1000L, 'B'),
                entry(1001L, 'C')
        ));

        when(courseServiceClient.getCorrectAnswers(anyLong())).thenReturn(
                new QuizCorrectAnswersResponse(100L, 10L, answers)
        );
        lessonService.submitLessonProgress(userId, new QuizSubmitRequest(100L, answers));

        ListEnrollmentResponses responses = enrollmentService.getEnrollmentsByUserId(userId);
        verify(courseServiceClient, times(2)).getAllCoursesByIds(List.of(10L));
        assertThat(responses.enrollmentResponses().getFirst().completedLessons()).isEqualTo(1);
    }
}