package com.yurii.zhuravlov.learningservice.mq;

import com.yurii.zhuravlov.eventsDto.CourseUpdatedEvent;
import com.yurii.zhuravlov.eventsDto.enums.CourseAction;
import com.yurii.zhuravlov.learningservice.repo.EnrolmentRepository;
import com.yurii.zhuravlov.learningservice.repo.UserLessonProgressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class CourseUpdateListenerTest {
    @Mock
    private EnrolmentRepository enrolmentRepository;

    @Mock
    private UserLessonProgressRepository userLessonProgressRepository;

    @InjectMocks
    private CourseUpdateListener courseUpdateListener;

    @Test
    void handleCourseUpdate_WhenAddLesson() {
        // Given
        CourseUpdatedEvent event = new CourseUpdatedEvent(
                1L, null, 10, CourseAction.ADD_LESSON);

        // When
        courseUpdateListener.handleCourseUpdate(event);

        // Then
        verify(enrolmentRepository).addLessonAndUpdateStatus(1L, 10);
    }

    @Test
    void handleCourseUpdate_WhenRemoveLesson() {
        // Given
        CourseUpdatedEvent event = new CourseUpdatedEvent(
                1L, 2L, 10, CourseAction.REMOVE_LESSON);

        // When
        courseUpdateListener.handleCourseUpdate(event);

        // Then
        verify(enrolmentRepository).updateTotalLessons(1L, 10);
        verify(userLessonProgressRepository).deleteByLessonId(2L);
    }

    @Test
    void handleCourseUpdate_WhenUpdateLessonQuiz() {
        // Given
        CourseUpdatedEvent event = new CourseUpdatedEvent(
                1L, 2L, 10, CourseAction.UPDATE_LESSON_QUIZ);

        // When
        courseUpdateListener.handleCourseUpdate(event);

        // Then
        verify(enrolmentRepository).updateStatusWithUpdates(1L);
        verify(userLessonProgressRepository).deleteByLessonId(2L);

    }

    @Test
    void handleCourseUpdate_WhenRemoveCourse() {
        // Given
        CourseUpdatedEvent event = new CourseUpdatedEvent(
                1L, null, 10, CourseAction.REMOVE_COURSE);

        // When
        courseUpdateListener.handleCourseUpdate(event);

        // Then
        verify(enrolmentRepository).deleteByCourseId(1L);
    }
}