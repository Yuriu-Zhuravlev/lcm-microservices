package com.yurii.zhuravlov.courseservice.mq;

import com.yurii.zhuravlov.courseservice.config.RabbitConfig;
import com.yurii.zhuravlov.eventsDto.CourseUpdatedEvent;
import com.yurii.zhuravlov.eventsDto.enums.CourseAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private CourseEventPublisher courseEventPublisher;

    @Test
    void publishAddLesson_ShouldSendCorrectEvent() {
        Long courseId = 1L;
        int totalLessons = 5;

        courseEventPublisher.publishAddLesson(courseId, totalLessons);

        ArgumentCaptor<CourseUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(CourseUpdatedEvent.class);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.COURSE_EXCHANGE),
                eq(""),
                eventCaptor.capture()
        );

        CourseUpdatedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.courseId()).isEqualTo(courseId);
        assertThat(capturedEvent.newTotalLessons()).isEqualTo(totalLessons);
        assertThat(capturedEvent.action()).isEqualTo(CourseAction.ADD_LESSON);
    }

    @Test
    void publishRemoveLesson_ShouldSendCorrectEvent() {
        Long courseId = 1L;
        Long lessonId = 10L;
        int totalLessons = 4;

        courseEventPublisher.publishRemoveLesson(courseId, lessonId, totalLessons);

        ArgumentCaptor<CourseUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(CourseUpdatedEvent.class);
        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.COURSE_EXCHANGE), eq(""), eventCaptor.capture());

        CourseUpdatedEvent event = eventCaptor.getValue();
        assertThat(event.courseId()).isEqualTo(courseId);
        assertThat(event.lessonId()).isEqualTo(lessonId);
        assertThat(event.newTotalLessons()).isEqualTo(totalLessons);
        assertThat(event.action()).isEqualTo(CourseAction.REMOVE_LESSON);
    }

    @Test
    void publishUpdateQuiz_ShouldSendCorrectEvent() {
        Long lessonId = 10L;
        Long courseId = 1L;

        courseEventPublisher.publishUpdateQuiz(lessonId, courseId);

        ArgumentCaptor<CourseUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(CourseUpdatedEvent.class);
        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.COURSE_EXCHANGE), eq(""), eventCaptor.capture());

        assertThat(eventCaptor.getValue().action()).isEqualTo(CourseAction.UPDATE_LESSON_QUIZ);
        assertThat(eventCaptor.getValue().lessonId()).isEqualTo(lessonId);
    }

    @Test
    void publishRemoveCourse_ShouldSendCorrectEvent() {
        Long courseId = 1L;

        courseEventPublisher.publishRemoveCourse(courseId);

        ArgumentCaptor<CourseUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(CourseUpdatedEvent.class);
        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.COURSE_EXCHANGE), eq(""), eventCaptor.capture());

        assertThat(eventCaptor.getValue().action()).isEqualTo(CourseAction.REMOVE_COURSE);
        assertThat(eventCaptor.getValue().courseId()).isEqualTo(courseId);
    }
}