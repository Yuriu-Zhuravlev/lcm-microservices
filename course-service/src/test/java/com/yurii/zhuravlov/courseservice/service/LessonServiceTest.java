package com.yurii.zhuravlov.courseservice.service;


import com.yurii.zhuravlov.courseservice.exception.CourseNotFoundException;
import com.yurii.zhuravlov.courseservice.exception.LessonNotFoundException;
import com.yurii.zhuravlov.courseservice.exception.NotAnAuthorException;
import com.yurii.zhuravlov.courseservice.model.Course;
import com.yurii.zhuravlov.courseservice.model.Lesson;
import com.yurii.zhuravlov.courseservice.model.Option;
import com.yurii.zhuravlov.courseservice.model.Question;
import com.yurii.zhuravlov.courseservice.mq.CourseEventPublisher;
import com.yurii.zhuravlov.courseservice.repo.CourseRepository;
import com.yurii.zhuravlov.courseservice.repo.LessonRepository;
import com.yurii.zhuravlov.requests.LessonCreteRequest;
import com.yurii.zhuravlov.requests.LessonUpdateRequest;
import com.yurii.zhuravlov.responses.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseEventPublisher courseEventPublisher;

    @InjectMocks
    private LessonService lessonService;

    @Test
    void createLesson_Success() {
        Long userId = 1L;
        Long courseId = 10L;
        LessonCreteRequest request = new LessonCreteRequest("New Lesson", "<p>Content</p>", 1, courseId);

        Course course = Course.builder().id(courseId).authorId(userId).build();
        Lesson savedLesson = Lesson.builder()
                .id(100L).title("New Lesson").course(course).orderIndex(1)
                .build();

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(lessonRepository.save(any(Lesson.class))).thenReturn(savedLesson);
        when(lessonRepository.countByCourseId(courseId)).thenReturn(5);

        LessonResponseFull response = lessonService.createLesson(request, userId);

        assertThat(response.title()).isEqualTo("New Lesson");
        verify(courseEventPublisher).publishAddLesson(courseId, 5);
    }

    @Test
    void createLesson_CourseNotFoundException() {
        Long userId = 1L;
        Long courseId = 10L;
        LessonCreteRequest request = new LessonCreteRequest("New Lesson", "<p>Content</p>", 1, courseId);



        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        assertThrows(CourseNotFoundException.class, () -> lessonService.createLesson(request, userId));
    }

    @Test
    void createLesson_NotAnAuthorException() {
        Long userId = 1L;
        Long courseId = 10L;
        LessonCreteRequest request = new LessonCreteRequest("New Lesson", "<p>Content</p>", 1, courseId);
        Course course = Course.builder().id(courseId).authorId(2L).build();

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        assertThrows(NotAnAuthorException.class, () -> lessonService.createLesson(request, userId));
    }

    @Test
    void deleteLesson_ShouldPublishRemoveEvent() {
        Long userId = 1L;
        Long lessonId = 100L;
        Long courseId = 10L;
        Course course = Course.builder().id(courseId).authorId(userId).build();
        Lesson lesson = Lesson.builder().id(lessonId).course(course).build();

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(lessonRepository.countByCourseId(courseId)).thenReturn(4);

        lessonService.deleteLesson(lessonId, userId);

        verify(lessonRepository).delete(lesson);
        verify(courseEventPublisher).publishRemoveLesson(courseId, lessonId, 4);
    }

    @Test
    void deleteLesson_NotAnAuthor() {
        Long userId = 1L;
        Long lessonId = 100L;
        Long courseId = 10L;
        Course course = Course.builder().id(courseId).authorId(2L).build();
        Lesson lesson = Lesson.builder().id(lessonId).course(course).build();

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));

        assertThrows(NotAnAuthorException.class, () ->  lessonService.deleteLesson(lessonId, userId));

        verify(lessonRepository, never()).delete(any(Lesson.class));
        verify(courseEventPublisher, never()).publishRemoveLesson(anyLong(), anyLong(), anyByte());
    }

    @Test
    void getCorrectAnswers_ShouldReturnMapOfLetters() {
        Long lessonId = 1L;
        Course course = Course.builder().id(10L).build();

        Question question = new Question();
        question.setId(500L);
        question.setOptions(Map.of(
                'A', new Option("Wrong", false),
                'B', new Option("Correct!", true)
        ));

        Lesson lesson = Lesson.builder()
                .id(lessonId)
                .course(course)
                .questions(List.of(question))
                .build();

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));

        QuizCorrectAnswersResponse result = lessonService.getCorrectAnswers(lessonId);

        assertThat(result.lessonId()).isEqualTo(lessonId);
        assertThat(result.answers().get(500L)).isEqualTo('B');
    }

    @Test
    void getCorrectAnswers_NoCorrectAnswer_ShouldThrowException() {
        Long lessonId = 1L;
        Course course = Course.builder().id(10L).build();

        Question question = new Question();
        question.setId(500L);
        question.setOptions(Map.of(
                'A', new Option("Wrong", false),
                'B', new Option("Wrong2", false)
        ));

        Lesson lesson = Lesson.builder()
                .id(lessonId)
                .course(course)
                .questions(List.of(question))
                .build();

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));

        assertThrows(IllegalStateException.class,() -> lessonService.getCorrectAnswers(lessonId));
    }

    @Test
    void getLessonById_WhenUserIsNotAuthor_ShouldThrowException() {
        Long lessonId = 1L;
        Long hackerId = 999L;
        Course course = Course.builder().authorId(1L).build();
        Lesson lesson = Lesson.builder().course(course).build();

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));

        assertThrows(NotAnAuthorException.class, () -> lessonService.getLessonById(lessonId, hackerId));
    }

    @Test
    void getLessonById_ShouldReturnResponse() {
        Long lessonId = 1L;
        Course course = Course.builder().authorId(1L).build();

        Question question = new Question();
        question.setId(500L);
        question.setOptions(Map.of(
                'A', new Option("Wrong", false),
                'B', new Option("Correct", true)
        ));

        Lesson lesson = Lesson.builder()
                .id(lessonId)
                .course(course)
                .questions(List.of(question))
                .build();


        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));

        LessonResponseFull response =  lessonService.getLessonById(lessonId, 1L);

        assertThat(response.questions().getFirst().options().get('A').isCorrect()).isFalse();
        assertThat(response.questions().getFirst().options().get('B').isCorrect()).isTrue();

    }

    @Test
    void getLessonByIdInternal_ShouldReturnResponse() {
        Long lessonId = 1L;
        Course course = Course.builder().authorId(1L).build();

        Question question = new Question();
        question.setId(500L);
        question.setOptions(Map.of(
                'A', new Option("Wrong", false),
                'B', new Option("Correct", true)
        ));

        Lesson lesson = Lesson.builder()
                .id(lessonId)
                .course(course)
                .questions(List.of(question))
                .build();


        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));

        LessonResponseFull response =  lessonService.getLessonByIdInternal(lessonId);

        assertThat(response.questions().getFirst().options().get('A').isCorrect()).isNull();
        assertThat(response.questions().getFirst().options().get('B').isCorrect()).isNull();

    }

    @Test
    void getLessonById_WhenLessonNotFound_ShouldThrowException() {
        Long lessonId = 1L;
        Long hackerId = 999L;

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.empty());

        assertThrows(LessonNotFoundException.class, () -> lessonService.getLessonById(lessonId, hackerId));
    }

    @Test
    void updateLesson_Success_ShouldUpdateAllFields() {
        // Given
        Long lessonId = 1L;
        Long userId = 10L;
        LessonUpdateRequest request = new LessonUpdateRequest("Updated Title", "Updated HTML", 5);

        Course course = Course.builder().id(2L).authorId(userId).build();
        Lesson existingLesson = Lesson.builder()
                .id(lessonId)
                .title("Old Title")
                .htmlContent("Old HTML")
                .orderIndex(1)
                .course(course)
                .build();

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(existingLesson));
        // При save повертаємо той самий об'єкт (він уже змінений сетерами)
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        LessonResponseFull response = lessonService.updateLesson(lessonId, request, userId);

        // Then
        assertThat(response.title()).isEqualTo("Updated Title");
        assertThat(response.htmlContent()).isEqualTo("Updated HTML");
        assertThat(response.orderIndex()).isEqualTo(5);

        // Перевіряємо, чи дійсно сетери відпрацювали на об'єкті перед збереженням
        verify(lessonRepository).save(existingLesson);
        assertThat(existingLesson.getTitle()).isEqualTo("Updated Title");
    }

    @Test
    void updateLesson_WhenLessonNotFound_ShouldThrowLessonNotFoundException() {
        // Given
        Long lessonId = 999L;
        Long userId = 1L;
        LessonUpdateRequest request = new LessonUpdateRequest("Title", "Content", 1);

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(LessonNotFoundException.class,
                () -> lessonService.updateLesson(lessonId, request, userId));

        verify(lessonRepository, never()).save(any());
    }

    @Test
    void updateLesson_WhenUserIsNotAuthor_ShouldThrowNotAnAuthorException() {
        // Given
        Long lessonId = 1L;
        Long realAuthorId = 10L;
        Long hackerId = 777L;

        Course course = Course.builder().id(2L).authorId(realAuthorId).build();
        Lesson lesson = Lesson.builder().id(lessonId).course(course).build();

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));

        // When & Then
        assertThrows(NotAnAuthorException.class,
                () -> lessonService.updateLesson(lessonId, new LessonUpdateRequest("T", "H", 1), hackerId));

        verify(lessonRepository, never()).save(any());
    }
}