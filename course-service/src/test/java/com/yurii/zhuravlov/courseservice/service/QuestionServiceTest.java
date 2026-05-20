package com.yurii.zhuravlov.courseservice.service;


import com.yurii.zhuravlov.courseservice.exception.LessonNotFoundException;
import com.yurii.zhuravlov.courseservice.exception.NotAnAuthorException;
import com.yurii.zhuravlov.courseservice.exception.QuestionNotFoundException;
import com.yurii.zhuravlov.courseservice.model.Course;
import com.yurii.zhuravlov.courseservice.model.Lesson;
import com.yurii.zhuravlov.courseservice.model.Question;
import com.yurii.zhuravlov.courseservice.mq.CourseEventPublisher;
import com.yurii.zhuravlov.courseservice.repo.LessonRepository;
import com.yurii.zhuravlov.courseservice.repo.QuestionRepository;
import com.yurii.zhuravlov.requests.OptionRequest;
import com.yurii.zhuravlov.requests.QuestionRequest;
import com.yurii.zhuravlov.responses.QuestionResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private CourseEventPublisher courseEventPublisher;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private QuestionService questionService;

    @Nested
    class CreateQuestionTests {
        @Test
        void createQuestion_Success() {
            Long lessonId = 1L;
            Long userId = 10L;
            Long courseId = 5L;
            QuestionRequest request = new QuestionRequest("What is Java?", List.of(
                    new OptionRequest("Language", true),
                    new OptionRequest("Coffee", false)
            ));

            Course course = Course.builder().id(courseId).authorId(userId).build();
            Lesson lesson = Lesson.builder().id(lessonId).course(course).build();

            when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
            when(questionRepository.save(any(Question.class))).thenAnswer(i -> {
                Question q = i.getArgument(0);
                q.setId(100L);
                return q;
            });

            QuestionResponse response = questionService.createQuestion(request, lessonId, userId);

            assertThat(response.text()).isEqualTo("What is Java?");
            verify(questionRepository).save(any(Question.class));
            verify(courseEventPublisher).publishUpdateQuiz(lessonId, courseId);
        }

        @Test
        void createQuestion_WhenNotAuthor_ShouldThrowException() {
            Long lessonId = 1L;
            Long userId = 10L;
            Course course = Course.builder().authorId(999L).build();
            Lesson lesson = Lesson.builder().course(course).build();

            when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));

            assertThrows(NotAnAuthorException.class,
                    () -> questionService.createQuestion(null, lessonId, userId));
        }

        @Test
        void createQuestion_WhenLessonNotFound_ShouldThrowException() {
            Long lessonId = 1L;
            Long userId = 10L;

            when(lessonRepository.findById(lessonId)).thenReturn(Optional.empty());

            assertThrows(LessonNotFoundException.class,
                    () -> questionService.createQuestion(null, lessonId, userId));
        }
    }

    @Nested
    class UpdateQuestionTests {
        @Test
        void updateQuestion_Success() {
            Long questionId = 1L;
            Long userId = 10L;
            QuestionRequest request = new QuestionRequest("New Text", List.of(
                    new OptionRequest("Opt 1", true),
                    new OptionRequest("Opt 2", false)
            ));

            Course course = Course.builder().id(5L).authorId(userId).build();
            Lesson lesson = Lesson.builder().id(2L).course(course).build();
            Question existingQuestion = new Question();
            existingQuestion.setId(questionId);
            existingQuestion.setLesson(lesson);

            when(questionRepository.findByIdWithCourse(questionId)).thenReturn(Optional.of(existingQuestion));
            when(questionRepository.save(any(Question.class))).thenReturn(existingQuestion);

            QuestionResponse response = questionService.updateQuestion(request, questionId, userId);

            assertThat(response.text()).isEqualTo("New Text");
            verify(courseEventPublisher).publishUpdateQuiz(2L, 5L);
            verify(redisTemplate, times(2)).delete(any(String.class));
        }

        @Test
        void updateQuestion_WhenNotAnAuthor_ShouldThrowException() {
            Long questionId = 1L;
            Long userId = 10L;
            QuestionRequest request = new QuestionRequest("New Text", List.of(
                    new OptionRequest("Opt 1", true),
                    new OptionRequest("Opt 2", false)
            ));

            Course course = Course.builder().id(5L).authorId(12L).build();
            Lesson lesson = Lesson.builder().id(2L).course(course).build();
            Question existingQuestion = new Question();
            existingQuestion.setId(questionId);
            existingQuestion.setLesson(lesson);

            when(questionRepository.findByIdWithCourse(questionId)).thenReturn(Optional.of(existingQuestion));

            assertThrows(NotAnAuthorException.class,
                    () -> questionService.updateQuestion(request, questionId, userId));
        }

        @Test
        void updateQuestion_WhenNotFound_ShouldThrowException() {
            when(questionRepository.findByIdWithCourse(anyLong())).thenReturn(Optional.empty());

            assertThrows(QuestionNotFoundException.class,
                    () -> questionService.updateQuestion(null, 1L, 1L));
        }
    }

    @Nested
    class DeleteQuestionTests {
        @Test
        void deleteQuestion_Success() {
            Long questionId = 1L;
            Long userId = 10L;
            Course course = Course.builder().id(5L).authorId(userId).build();
            Lesson lesson = Lesson.builder().id(2L).course(course).build();
            Question question = new Question();
            question.setLesson(lesson);

            when(questionRepository.findByIdWithCourse(questionId)).thenReturn(Optional.of(question));

            questionService.deleteQuestion(questionId, userId);

            verify(questionRepository).delete(question);
            verify(courseEventPublisher).publishUpdateQuiz(2L, 5L);
            verify(redisTemplate, times(2)).delete(any(String.class));
        }

        @Test
        void deleteQuestion_WhenUserNotAuthor_ShouldThrowException() {
            Long questionId = 1L;
            Course course = Course.builder().authorId(999L).build();
            Lesson lesson = Lesson.builder().course(course).build();
            Question question = new Question();
            question.setLesson(lesson);

            when(questionRepository.findByIdWithCourse(questionId)).thenReturn(Optional.of(question));

            assertThrows(NotAnAuthorException.class,
                    () -> questionService.deleteQuestion(questionId, 10L));
        }

        @Test
        void deleteQuestion_QuestionNotFound_ShouldThrowException() {
            Long questionId = 1L;

            when(questionRepository.findByIdWithCourse(questionId)).thenReturn(Optional.empty());

            assertThrows(QuestionNotFoundException.class,
                    () -> questionService.deleteQuestion(questionId, 10L));
        }
    }
}