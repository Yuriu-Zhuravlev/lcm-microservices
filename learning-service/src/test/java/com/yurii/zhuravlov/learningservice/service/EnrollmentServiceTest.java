package com.yurii.zhuravlov.learningservice.service;

import com.yurii.zhuravlov.learningservice.client.CourseServiceClient;
import com.yurii.zhuravlov.learningservice.dto.EnrolmentWithProgressDTO;
import com.yurii.zhuravlov.learningservice.exceptions.AlreadyEnrolledException;
import com.yurii.zhuravlov.learningservice.exceptions.CourseNotCompletedException;
import com.yurii.zhuravlov.learningservice.exceptions.CourseNotFoundException;
import com.yurii.zhuravlov.learningservice.exceptions.NotEnrolledException;
import com.yurii.zhuravlov.learningservice.model.Enrolment;
import com.yurii.zhuravlov.learningservice.model.UserLessonProgress;
import com.yurii.zhuravlov.learningservice.model.enums.EnrolmentStatus;
import com.yurii.zhuravlov.learningservice.repo.EnrolmentRepository;
import com.yurii.zhuravlov.responses.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrolmentRepository enrolmentRepository;

    @Mock
    private CourseServiceClient courseServiceClient;

    @InjectMocks
    private EnrollmentService enrollmentService;

    @Test
    void enrollUser_Success() {
        Long userId = 1L, courseId = 2L;
        CourseResponseShort courseDto = new CourseResponseShort(courseId, "Title", "Desc", new UserResponse(1L, "U"), 10);

        when(enrolmentRepository.existsByUserIdAndCourseId(userId, courseId)).thenReturn(false);
        when(courseServiceClient.getCourseShortById(courseId)).thenReturn(courseDto);
        when(enrolmentRepository.save(any(Enrolment.class))).thenAnswer(i -> i.getArgument(0));

        EnrollmentResponse response = enrollmentService.enrollUser(userId, courseId);

        assertThat(response.enrollmentStatus()).isEqualTo("ENROLLED");
        assertThat(response.totalLessons()).isEqualTo(10);
        verify(enrolmentRepository).save(any(Enrolment.class));
    }

    @Test
    void enrollUser_AlreadyEnrolled_ThrowsException() {
        when(enrolmentRepository.existsByUserIdAndCourseId(1L, 2L)).thenReturn(true);
        assertThrows(AlreadyEnrolledException.class, () -> enrollmentService.enrollUser(1L, 2L));
    }

    @Test
    void getEnrollmentById_Success_WithProgress() {
        Long userId = 1L, enrolmentId = 10L;
        Enrolment enrolment = Enrolment.builder()
                .id(enrolmentId).userId(userId).courseId(100L)
                .status(EnrolmentStatus.ENROLLED).enrolledAt(LocalDateTime.now())
                .lessonsProgress(List.of(
                        UserLessonProgress.builder().lessonId(1L).isCompleted(true).correctAnswers(5).totalQuestions(5).build()
                )).build();

        CourseResponseFull courseFull = new CourseResponseFull(100L, "Title", "Desc", new UserResponse(1L, "U"),
                List.of(new LessonResponseShort(1L, "L1", 1)));

        when(enrolmentRepository.findById(enrolmentId)).thenReturn(Optional.of(enrolment));
        when(courseServiceClient.getCourseById(100L)).thenReturn(courseFull);

        EnrollmentResponse response = enrollmentService.getEnrollmentById(userId, enrolmentId);

        assertThat(response.lessons()).hasSize(1);
        assertThat(response.lessons().getFirst().isCompleted()).isTrue();
    }

    @Test
    void getEnrollmentById_NotOwner_ThrowsException() {
        Enrolment enrolment = Enrolment.builder().userId(999L).build();
        when(enrolmentRepository.findById(1L)).thenReturn(Optional.of(enrolment));

        assertThrows(NotEnrolledException.class, () -> enrollmentService.getEnrollmentById(1L, 1L));
    }

    @Test
    void getEnrollmentById_NoLessons_ReturnsEmptyList() {
        Enrolment enrolment = Enrolment.builder().id(1L).userId(1L).courseId(2L).status(EnrolmentStatus.ENROLLED).build();
        CourseResponseFull courseFull = new CourseResponseFull(2L, "T", "D", new UserResponse(1L, "U"), null);

        when(enrolmentRepository.findById(1L)).thenReturn(Optional.of(enrolment));
        when(courseServiceClient.getCourseById(2L)).thenReturn(courseFull);

        EnrollmentResponse response = enrollmentService.getEnrollmentById(1L, 1L);
        assertThat(response.lessons()).isNull();
    }

    @Test
    void getEnrollmentById_CourseHasNoLessons() {
        Long userId = 1L, enrolmentId = 10L;
        Enrolment enrolment = Enrolment.builder()
                .id(enrolmentId).userId(userId).courseId(100L)
                .status(EnrolmentStatus.ENROLLED).build();

        CourseResponseFull courseFull = new CourseResponseFull(100L, "Title", "Desc", new UserResponse(1L, "U"), null);

        when(enrolmentRepository.findById(enrolmentId)).thenReturn(Optional.of(enrolment));
        when(courseServiceClient.getCourseById(100L)).thenReturn(courseFull);

        EnrollmentResponse response = enrollmentService.getEnrollmentById(userId, enrolmentId);

        assertThat(response.lessons()).isNull();
        assertThat(response.course().totalLessonsCount()).isEqualTo(0);
    }

    @Test
    void getEnrollmentById_NoProgressStarted() {
        Long userId = 1L, enrolmentId = 10L;
        Enrolment enrolment = Enrolment.builder()
                .id(enrolmentId).userId(userId).courseId(100L)
                .status(EnrolmentStatus.ENROLLED)
                .lessonsProgress(null)
                .build();

        List<LessonResponseShort> lessons = List.of(new LessonResponseShort(1L, "Title", 0));
        CourseResponseFull courseFull = new CourseResponseFull(100L, "T", "D", new UserResponse(1L, "U"), lessons);

        when(enrolmentRepository.findById(enrolmentId)).thenReturn(Optional.of(enrolment));
        when(courseServiceClient.getCourseById(100L)).thenReturn(courseFull);

        EnrollmentResponse response = enrollmentService.getEnrollmentById(userId, enrolmentId);

        assertThat(response.lessons()).hasSize(1);
        assertThat(response.lessons().getFirst().correctAnswers()).isEqualTo(0);
        assertThat(response.lessons().getFirst().isCompleted()).isFalse();
    }

    @Test
    void getEnrollmentById_PartialProgress() {
        Long userId = 1L, enrolmentId = 10L;
        UserLessonProgress p1 = UserLessonProgress.builder()
                .lessonId(1L).isCompleted(true).correctAnswers(10).totalQuestions(10).build();

        Enrolment enrolment = Enrolment.builder()
                .id(enrolmentId).userId(userId).courseId(100L)
                .status(EnrolmentStatus.ENROLLED)
                .lessonsProgress(List.of(p1)).build();

        List<LessonResponseShort> lessons = List.of(
                new LessonResponseShort(1L, "L1", 0),
                new LessonResponseShort(2L, "L2", 1)
        );
        CourseResponseFull courseFull = new CourseResponseFull(100L, "T", "D", new UserResponse(1L, "U"), lessons);

        when(enrolmentRepository.findById(enrolmentId)).thenReturn(Optional.of(enrolment));
        when(courseServiceClient.getCourseById(100L)).thenReturn(courseFull);

        EnrollmentResponse response = enrollmentService.getEnrollmentById(userId, enrolmentId);

        assertThat(response.lessons()).hasSize(2);
        assertThat(response.lessons().get(0).isCompleted()).isTrue();
        assertThat(response.lessons().get(1).isCompleted()).isFalse();
        assertThat(response.lessons().get(1).correctAnswers()).isEqualTo(0);
    }

    @Test
    void getEnrollmentsByUserId_Success() {
        Long userId = 1L;
        Enrolment e = Enrolment.builder().id(10L).courseId(100L).status(EnrolmentStatus.ENROLLED).build();
        EnrolmentWithProgressDTO dto = new EnrolmentWithProgressDTO(e, 5L);
        CourseResponseShort course = new CourseResponseShort(100L, "T", "D", new UserResponse(1L, "U"), 10);

        when(enrolmentRepository.findByUserIdWithCompletedCount(userId)).thenReturn(List.of(dto));
        when(courseServiceClient.getAllCoursesByIds(any())).thenReturn(List.of(course));

        List<EnrollmentResponse> responses = enrollmentService.getEnrollmentsByUserId(userId);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().completedLessons()).isEqualTo(5);
    }

    @Test
    void getEnrollmentsByUserId_Empty_ReturnsEmptyList() {
        when(enrolmentRepository.findByUserIdWithCompletedCount(1L)).thenReturn(Collections.emptyList());
        List<EnrollmentResponse> responses = enrollmentService.getEnrollmentsByUserId(1L);
        assertThat(responses).isEmpty();
    }

    @Test
    void getEnrollmentsByUserId_CourseMissing_ThrowsException() {
        Enrolment e = Enrolment.builder().courseId(100L).build();
        when(enrolmentRepository.findByUserIdWithCompletedCount(1L)).thenReturn(List.of(new EnrolmentWithProgressDTO(e, 0L)));
        when(courseServiceClient.getAllCoursesByIds(any())).thenReturn(Collections.emptyList());

        assertThrows(CourseNotFoundException.class, () -> enrollmentService.getEnrollmentsByUserId(1L));
    }

    @Test
    void tryCompleteCourse_Success() {
        Long userId = 1L, enrolmentId = 10L;
        Enrolment enrolment = Enrolment.builder()
                .userId(userId).totalLessonsCount(5).status(EnrolmentStatus.ENROLLED).build();

        when(enrolmentRepository.findById(enrolmentId)).thenReturn(Optional.of(enrolment));
        when(enrolmentRepository.countCompletedLessons(enrolmentId)).thenReturn(5L);

        enrollmentService.tryCompleteCourse(userId, enrolmentId);

        assertThat(enrolment.getStatus()).isEqualTo(EnrolmentStatus.COMPLETED);
        assertThat(enrolment.getCompletedAt()).isNotNull();
        verify(enrolmentRepository).save(enrolment);
    }

    @Test
    void tryCompleteCourse_AlreadyCompleted_DoesNothing() {
        Enrolment enrolment = Enrolment.builder().userId(1L).status(EnrolmentStatus.COMPLETED).build();
        when(enrolmentRepository.findById(1L)).thenReturn(Optional.of(enrolment));

        enrollmentService.tryCompleteCourse(1L, 1L);

        verify(enrolmentRepository, never()).save(any());
    }

    @Test
    void tryCompleteCourse_NotEnoughLessons_ThrowsException() {
        Enrolment enrolment = Enrolment.builder().userId(1L).totalLessonsCount(5).build();
        when(enrolmentRepository.findById(1L)).thenReturn(Optional.of(enrolment));
        when(enrolmentRepository.countCompletedLessons(1L)).thenReturn(3L);

        assertThrows(CourseNotCompletedException.class, () -> enrollmentService.tryCompleteCourse(1L, 1L));
    }

    @Test
    void tryCompleteCourse_NotEnrolled_ThrowsException() {
        Enrolment enrolment = Enrolment.builder().userId(1L).totalLessonsCount(5).build();
        when(enrolmentRepository.findById(1L)).thenReturn(Optional.of(enrolment));

        assertThrows(NotEnrolledException.class, () -> enrollmentService.tryCompleteCourse(2L, 1L));
    }
}