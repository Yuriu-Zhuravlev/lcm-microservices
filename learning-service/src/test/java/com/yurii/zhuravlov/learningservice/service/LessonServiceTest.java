package com.yurii.zhuravlov.learningservice.service;

import com.yurii.zhuravlov.learningservice.client.CourseServiceClient;
import com.yurii.zhuravlov.learningservice.exceptions.NotEnrolledException;
import com.yurii.zhuravlov.learningservice.model.Enrolment;
import com.yurii.zhuravlov.learningservice.model.UserLessonProgress;
import com.yurii.zhuravlov.learningservice.repo.EnrolmentRepository;
import com.yurii.zhuravlov.learningservice.repo.UserLessonProgressRepository;
import com.yurii.zhuravlov.requests.QuizSubmitRequest;
import com.yurii.zhuravlov.responses.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

    @Mock
    private EnrolmentRepository enrolmentRepository;
    @Mock
    private CourseServiceClient courseServiceClient;
    @Mock
    private UserLessonProgressRepository userLessonProgressRepository;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private LessonService lessonService;

    // --- getLessonContent() ---

    @Test
    void getLessonContent_Success() {
        Long lessonId = 1L, userId = 2L, courseId = 3L;
        LessonResponseFull lesson = new LessonResponseFull(lessonId, "Title", "Content", 1, null, courseId);

        when(courseServiceClient.getLessonByIdInternal(lessonId)).thenReturn(lesson);
        when(enrolmentRepository.existsByUserIdAndCourseId(userId, courseId)).thenReturn(true);

        LessonResponseFull result = lessonService.getLessonContent(lessonId, userId);

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("Title");
    }

    @Test
    void getLessonContent_NotEnrolled_ThrowsException() {
        LessonResponseFull lesson = new LessonResponseFull(1L, "T", "C", 1, null, 3L);
        when(courseServiceClient.getLessonByIdInternal(1L)).thenReturn(lesson);
        when(enrolmentRepository.existsByUserIdAndCourseId(anyLong(), anyLong())).thenReturn(false);

        assertThrows(NotEnrolledException.class, () -> lessonService.getLessonContent(1L, 2L));
    }

    // --- submitLessonProgress() ---

    @Test
    void submitLessonProgress_Success_Over80Percent() {
        // 4 з 5 правильних = 80% (Поріг пройдено)
        Long userId = 1L, lessonId = 10L, enrolmentId = 100L;
        Map<Long, Character> answers = Map.of(1L, 'A', 2L, 'B', 3L, 'C', 4L, 'D', 5L, 'E');
        QuizSubmitRequest request = new QuizSubmitRequest(lessonId, answers);

        QuizCorrectAnswersResponse correctResp = new QuizCorrectAnswersResponse(lessonId, 200L, answers); // 100% correct
        Enrolment enrolment = Enrolment.builder().id(enrolmentId).build();

        when(courseServiceClient.getCorrectAnswers(lessonId)).thenReturn(correctResp);
        when(enrolmentRepository.findByUserIdAndCourseId(userId, 200L)).thenReturn(Optional.of(enrolment));
        when(userLessonProgressRepository.findByEnrolmentIdAndLessonId(enrolmentId, lessonId)).thenReturn(Optional.empty());

        QuizSubmitResponse response = lessonService.submitLessonProgress(userId, request);

        assertThat(response.isCompleted()).isTrue();
        assertThat(response.scorePercentage()).isEqualTo(100.00);
        verify(userLessonProgressRepository).save(any(UserLessonProgress.class));
        verify(redisTemplate).delete(any(String.class));
    }

    @Test
    void submitLessonProgress_Fail_Under80Percent() {
        // 1 з 2 правильних = 50%
        Map<Long, Character> studentAnswers = Map.of(1L, 'A', 2L, 'B');
        Map<Long, Character> correctAnswers = Map.of(1L, 'A', 2L, 'Z'); // Друга відповідь неправильна

        QuizSubmitRequest request = new QuizSubmitRequest(10L, studentAnswers);
        QuizCorrectAnswersResponse correctResp = new QuizCorrectAnswersResponse(200L, 10L, correctAnswers);
        Enrolment enrolment = Enrolment.builder().id(100L).build();

        when(courseServiceClient.getCorrectAnswers(10L)).thenReturn(correctResp);
        when(enrolmentRepository.findByUserIdAndCourseId(anyLong(), anyLong())).thenReturn(Optional.of(enrolment));
        when(userLessonProgressRepository.findByEnrolmentIdAndLessonId(any(), any())).thenReturn(Optional.empty());

        QuizSubmitResponse response = lessonService.submitLessonProgress(1L, request);

        assertThat(response.isCompleted()).isFalse();
        assertThat(response.scorePercentage()).isEqualTo(50.00);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void submitLessonProgress_EmptyQuiz_MarksCompleted() {
        // Випадок, коли в уроці немає питань (actualAnswers.isEmpty())
        QuizSubmitRequest request = new QuizSubmitRequest(10L, Map.of());
        QuizCorrectAnswersResponse correctResp = new QuizCorrectAnswersResponse(200L, 10L, Map.of());
        Enrolment enrolment = Enrolment.builder().id(100L).build();

        when(courseServiceClient.getCorrectAnswers(10L)).thenReturn(correctResp);
        when(enrolmentRepository.findByUserIdAndCourseId(any(), any())).thenReturn(Optional.of(enrolment));
        when(userLessonProgressRepository.findByEnrolmentIdAndLessonId(any(), any())).thenReturn(Optional.empty());

        QuizSubmitResponse response = lessonService.submitLessonProgress(1L, request);

        assertThat(response.isCompleted()).isTrue();
        assertThat(response.scorePercentage()).isEqualTo(100.00);
        verify(redisTemplate).delete(any(String.class));
    }

    @Test
    void submitLessonProgress_BetterResult_UpdatesProgress() {
        // Покращуємо результат з 1 до 2 правильних відповідей
        Long enrolmentId = 100L, lessonId = 10L;
        Enrolment enrolment = Enrolment.builder().id(enrolmentId).build();
        UserLessonProgress existingProgress = UserLessonProgress.builder()
                .enrolment(enrolment).lessonId(lessonId)
                .isCompleted(false).correctAnswers(1).totalQuestions(2).build();

        Map<Long, Character> answers = Map.of(1L, 'A', 2L, 'A');
        QuizSubmitRequest request = new QuizSubmitRequest(lessonId, answers);
        QuizCorrectAnswersResponse correctResp = new QuizCorrectAnswersResponse(200L, lessonId, answers);

        when(courseServiceClient.getCorrectAnswers(lessonId)).thenReturn(correctResp);
        when(enrolmentRepository.findByUserIdAndCourseId(any(), any())).thenReturn(Optional.of(enrolment));
        when(userLessonProgressRepository.findByEnrolmentIdAndLessonId(enrolmentId, lessonId))
                .thenReturn(Optional.of(existingProgress));

        lessonService.submitLessonProgress(1L, request);

        assertThat(existingProgress.getCorrectAnswers()).isEqualTo(2);
        assertThat(existingProgress.getIsCompleted()).isTrue();
        verify(userLessonProgressRepository).save(existingProgress);
        verify(redisTemplate).delete(any(String.class));
    }

    @Test
    void submitLessonProgress_UserNotEnrolled_ThrowsException() {
        when(courseServiceClient.getCorrectAnswers(any())).thenReturn(new QuizCorrectAnswersResponse(200L, 1L, Map.of()));
        when(enrolmentRepository.findByUserIdAndCourseId(any(), any())).thenReturn(Optional.empty());

        assertThrows(NotEnrolledException.class,
                () -> lessonService.submitLessonProgress(1L, new QuizSubmitRequest(1L, Map.of())));
    }

    @Test
    void submitLessonProgress_AlreadyCompleted_ShouldNotUpdateCompletedAt() {
        // Given
        Long userId = 1L;
        Long lessonId = 10L;
        Long courseId = 200L;
        Long enrolmentId = 100L;

        // Створюємо прогрес, який ВЖЕ має статус isCompleted = true
        LocalDateTime firstCompletionTime = LocalDateTime.now().minusDays(1);
        UserLessonProgress existingProgress = UserLessonProgress.builder()
                .enrolment(Enrolment.builder().id(enrolmentId).build())
                .lessonId(lessonId)
                .isCompleted(true) // ВЖЕ ЗАВЕРШЕНО
                .completedAt(firstCompletionTime)
                .correctAnswers(8)
                .totalQuestions(10)
                .build();

        Map<Long, Character> answers = Map.of(1L, 'A'); // спрощена відповідь
        QuizSubmitRequest request = new QuizSubmitRequest(lessonId, answers);
        QuizCorrectAnswersResponse correctResp = new QuizCorrectAnswersResponse(lessonId, courseId, answers);

        when(courseServiceClient.getCorrectAnswers(lessonId)).thenReturn(correctResp);
        when(enrolmentRepository.findByUserIdAndCourseId(userId, courseId))
                .thenReturn(Optional.of(existingProgress.getEnrolment()));
        when(userLessonProgressRepository.findByEnrolmentIdAndLessonId(enrolmentId, lessonId))
                .thenReturn(Optional.of(existingProgress));

        // When
        lessonService.submitLessonProgress(userId, request);

        // Then
        // Перевіряємо, що completedAt НЕ змінився (залишився вчорашнім)
        assertThat(existingProgress.getCompletedAt()).isEqualTo(firstCompletionTime);
        assertThat(existingProgress.getIsCompleted()).isTrue();
        verify(userLessonProgressRepository).save(existingProgress);
        verifyNoInteractions(redisTemplate);

    }
}