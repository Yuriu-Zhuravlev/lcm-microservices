package com.yurii.zhuravlov.learningservice.repo;

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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserLessonProgressRepositoryTest {
    @Autowired
    private EnrolmentRepository enrolmentRepository;

    @Autowired
    private UserLessonProgressRepository userLessonProgressRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldDeleteProgressByLessonId(){
        Enrolment enrolment = Enrolment.builder()
                .userId(1L).courseId(100L).status(EnrolmentStatus.ENROLLED)
                .enrolledAt(LocalDateTime.now()).totalLessonsCount(1).build();

        UserLessonProgress progress1 = UserLessonProgress.builder()
                .enrolment(enrolment).lessonId(500L).isCompleted(true)
                .totalQuestions(10).correctAnswers(10).build();

        UserLessonProgress progress12 = UserLessonProgress.builder()
                .enrolment(enrolment).lessonId(501L).isCompleted(true)
                .totalQuestions(10).correctAnswers(10).build();

        enrolment.setLessonsProgress((List.of(progress1, progress12)));
        Long enrolmentId1 = enrolmentRepository.save(enrolment).getId();

        Enrolment enrolment2 = Enrolment.builder()
                .userId(2L).courseId(100L).status(EnrolmentStatus.ENROLLED)
                .enrolledAt(LocalDateTime.now()).totalLessonsCount(1).build();

        UserLessonProgress progress2 = UserLessonProgress.builder()
                .enrolment(enrolment2).lessonId(500L).isCompleted(true)
                .totalQuestions(10).correctAnswers(10).build();

        enrolment2.setLessonsProgress((List.of(progress2)));
        Long enrolmentId2 = enrolmentRepository.save(enrolment2).getId();

        entityManager.flush();
        entityManager.clear();

        assertThat(userLessonProgressRepository.findByEnrolmentIdAndLessonId(enrolmentId1, 500L))
                .isPresent();
        assertThat(userLessonProgressRepository.findByEnrolmentIdAndLessonId(enrolmentId1, 501L))
                .isPresent();
        assertThat(userLessonProgressRepository.findByEnrolmentIdAndLessonId(enrolmentId2, 500L))
                .isPresent();

        userLessonProgressRepository.deleteByLessonId(500L);

        entityManager.flush();
        entityManager.clear();

        assertThat(userLessonProgressRepository.findByEnrolmentIdAndLessonId(enrolmentId1, 500L))
                .isNotPresent();
        assertThat(userLessonProgressRepository.findByEnrolmentIdAndLessonId(enrolmentId1, 501L))
                .isPresent();
        assertThat(userLessonProgressRepository.findByEnrolmentIdAndLessonId(enrolmentId2, 500L))
                .isNotPresent();
    }
}
