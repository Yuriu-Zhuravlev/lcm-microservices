package com.yurii.zhuravlov.learningservice.repo;

import com.yurii.zhuravlov.learningservice.dto.EnrolmentWithProgressDTO;
import com.yurii.zhuravlov.learningservice.model.Enrolment;
import com.yurii.zhuravlov.learningservice.model.UserLessonProgress;
import com.yurii.zhuravlov.learningservice.model.enums.EnrolmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EnrolmentRepositoryTest {

    @Autowired
    private EnrolmentRepository enrolmentRepository;

    @Autowired
    private UserLessonProgressRepository userLessonProgressRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldUpdateTotalLessonsAndStatusWhenAddLesson() {
        Enrolment enrolment = Enrolment.builder()
                .userId(1L)
                .courseId(10L)
                .status(EnrolmentStatus.COMPLETED)
                .enrolledAt(LocalDateTime.now())
                .totalLessonsCount(5)
                .build();
        Enrolment enrolment2 = Enrolment.builder()
                .userId(2L)
                .courseId(10L)
                .status(EnrolmentStatus.ENROLLED)
                .enrolledAt(LocalDateTime.now())
                .totalLessonsCount(5)
                .build();
        enrolmentRepository.save(enrolment);
        enrolmentRepository.save(enrolment2);

        enrolmentRepository.addLessonAndUpdateStatus(10L, 6);

        entityManager.flush();
        entityManager.clear();

        assertThat(enrolmentRepository.existsByUserIdAndCourseId(1L, 10L)).isTrue();
        assertThat(enrolmentRepository.existsByUserIdAndCourseId(2L, 10L)).isTrue();


        Optional<Enrolment> updated = enrolmentRepository.findByUserIdAndCourseId(1L, 10L);
        Optional<Enrolment> updated2 = enrolmentRepository.findByUserIdAndCourseId(2L, 10L);

        assertThat(updated).isPresent();
        assertThat(updated.get().getTotalLessonsCount()).isEqualTo(6);
        assertThat(updated.get().getStatus()).isEqualTo(EnrolmentStatus.COMPLETED_WITH_UPDATES);

        assertThat(updated2).isPresent();
        assertThat(updated2.get().getTotalLessonsCount()).isEqualTo(6);
        assertThat(updated2.get().getStatus()).isEqualTo(EnrolmentStatus.ENROLLED);
    }

    @Test
    void shouldUpdateStatusWhenLessonUpdate() {
        Enrolment enrolment = Enrolment.builder()
                .userId(1L)
                .courseId(10L)
                .status(EnrolmentStatus.COMPLETED)
                .enrolledAt(LocalDateTime.now())
                .totalLessonsCount(5)
                .build();
        Enrolment enrolment2 = Enrolment.builder()
                .userId(2L)
                .courseId(10L)
                .status(EnrolmentStatus.ENROLLED)
                .enrolledAt(LocalDateTime.now())
                .totalLessonsCount(5)
                .build();
        enrolmentRepository.save(enrolment);
        enrolmentRepository.save(enrolment2);

        enrolmentRepository.updateStatusWithUpdates(10L);

        entityManager.flush();
        entityManager.clear();

        assertThat(enrolmentRepository.existsByUserIdAndCourseId(1L, 10L)).isTrue();
        assertThat(enrolmentRepository.existsByUserIdAndCourseId(2L, 10L)).isTrue();


        Optional<Enrolment> updated = enrolmentRepository.findByUserIdAndCourseId(1L, 10L);
        Optional<Enrolment> updated2 = enrolmentRepository.findByUserIdAndCourseId(2L, 10L);

        assertThat(updated).isPresent();
        assertThat(updated.get().getTotalLessonsCount()).isEqualTo(5);
        assertThat(updated.get().getStatus()).isEqualTo(EnrolmentStatus.COMPLETED_WITH_UPDATES);

        assertThat(updated2).isPresent();
        assertThat(updated2.get().getTotalLessonsCount()).isEqualTo(5);
        assertThat(updated2.get().getStatus()).isEqualTo(EnrolmentStatus.ENROLLED);
    }

    @Test
    void shouldUpdateTotalLessonsWhenRemoveLesson() {
        Enrolment enrolment = Enrolment.builder()
                .userId(1L)
                .courseId(10L)
                .status(EnrolmentStatus.ENROLLED)
                .enrolledAt(LocalDateTime.now())
                .totalLessonsCount(5)
                .build();
        enrolmentRepository.save(enrolment);

        enrolmentRepository.updateTotalLessons(10L, 4);

        entityManager.flush();
        entityManager.clear();

        assertThat(enrolmentRepository.existsByUserIdAndCourseId(1L, 10L)).isTrue();

        Optional<Enrolment> updated = enrolmentRepository.findByUserIdAndCourseId(1L, 10L);

        assertThat(updated).isPresent();
        assertThat(updated.get().getTotalLessonsCount()).isEqualTo(4);
        assertThat(updated.get().getStatus()).isEqualTo(EnrolmentStatus.ENROLLED);
    }

    @Test
    void shouldCountCompletedLessonsCorrectly() {
        Enrolment enrolment = Enrolment.builder()
                .userId(1L).courseId(1L).status(EnrolmentStatus.ENROLLED)
                .enrolledAt(LocalDateTime.now()).totalLessonsCount(2).build();
        
        UserLessonProgress p1 = UserLessonProgress.builder()
                .enrolment(enrolment).lessonId(101L).isCompleted(true)
                .totalQuestions(5).correctAnswers(5).build();
        UserLessonProgress p2 = UserLessonProgress.builder()
                .enrolment(enrolment).lessonId(102L).isCompleted(false)
                .totalQuestions(5).correctAnswers(0).build();
        
        enrolment.setLessonsProgress(List.of(p1, p2));
        enrolmentRepository.save(enrolment);

        Long completedCount = enrolmentRepository.countCompletedLessons(enrolment.getId());

        assertThat(completedCount).isEqualTo(1L);
    }

    @Test
    void shouldCountCompletedLessonsCorrectlyForMultipleEnrollments() {
        Enrolment enrolment = Enrolment.builder()
                .userId(1L).courseId(1L).status(EnrolmentStatus.ENROLLED)
                .enrolledAt(LocalDateTime.now()).totalLessonsCount(2).build();

        UserLessonProgress p1 = UserLessonProgress.builder()
                .enrolment(enrolment).lessonId(101L).isCompleted(true)
                .totalQuestions(5).correctAnswers(5).build();
        UserLessonProgress p2 = UserLessonProgress.builder()
                .enrolment(enrolment).lessonId(102L).isCompleted(true)
                .totalQuestions(5).correctAnswers(5).build();

        Enrolment enrolment2 = Enrolment.builder()
                .userId(1L).courseId(2L).status(EnrolmentStatus.ENROLLED)
                .enrolledAt(LocalDateTime.now()).totalLessonsCount(2).build();

        UserLessonProgress p12 = UserLessonProgress.builder()
                .enrolment(enrolment2).lessonId(101L).isCompleted(true)
                .totalQuestions(5).correctAnswers(5).build();
        UserLessonProgress p22 = UserLessonProgress.builder()
                .enrolment(enrolment2).lessonId(102L).isCompleted(false)
                .totalQuestions(5).correctAnswers(0).build();

        enrolment.setLessonsProgress(List.of(p1, p2));
        enrolment2.setLessonsProgress(List.of(p12, p22));
        enrolmentRepository.save(enrolment);
        enrolmentRepository.save(enrolment2);
        List<Long> result = enrolmentRepository.findByUserIdWithCompletedCount(1L).stream()
                .map(EnrolmentWithProgressDTO::completedLessonsCount)
                .toList();

        assertThat(result).contains(1L, 2L);
    }

    @Test
    void shouldDeleteEnrolmentAndCascadeDeleteProgress() {
        Enrolment enrolment = Enrolment.builder()
                .userId(1L).courseId(100L).status(EnrolmentStatus.ENROLLED)
                .enrolledAt(LocalDateTime.now()).totalLessonsCount(1).build();

        UserLessonProgress progress = UserLessonProgress.builder()
                .enrolment(enrolment).lessonId(500L).isCompleted(true)
                .totalQuestions(10).correctAnswers(10).build();

        enrolment.setLessonsProgress((List.of(progress)));
        enrolmentRepository.save(enrolment);

        entityManager.flush();
        entityManager.clear();

        assertThat(enrolmentRepository.existsByUserIdAndCourseId(1L, 100L)).isTrue();

        enrolmentRepository.deleteByCourseId(100L);

        entityManager.flush();
        entityManager.clear();

        Optional<Enrolment> deletedEnrolment = enrolmentRepository.findByUserIdAndCourseId(1L, 100L);
        assertThat(deletedEnrolment).isEmpty();

        assertThat(userLessonProgressRepository.findAll()).isEmpty();
    }
}