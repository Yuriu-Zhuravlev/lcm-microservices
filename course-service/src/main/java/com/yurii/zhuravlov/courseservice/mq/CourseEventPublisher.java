package com.yurii.zhuravlov.courseservice.mq;

import com.yurii.zhuravlov.courseservice.config.RabbitConfig;
import com.yurii.zhuravlov.eventsDto.CourseUpdatedEvent;
import com.yurii.zhuravlov.eventsDto.enums.CourseAction;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publishAddLesson(Long courseId, int totalLessons) {
        CourseUpdatedEvent event = CourseUpdatedEvent.builder()
                .courseId(courseId)
                .newTotalLessons(totalLessons)
                .action(CourseAction.ADD_LESSON)
                .build();
        rabbitTemplate.convertAndSend(RabbitConfig.COURSE_EXCHANGE, "", event);
    }

    public void publishRemoveLesson(Long courseId, Long lessonId, int totalLessons){
        CourseUpdatedEvent event = CourseUpdatedEvent.builder()
                .courseId(courseId)
                .lessonId(lessonId)
                .newTotalLessons(totalLessons)
                .action(CourseAction.REMOVE_LESSON)
                .build();
        rabbitTemplate.convertAndSend(RabbitConfig.COURSE_EXCHANGE, "", event);
    }

    public void publishUpdateQuiz(Long lessonId){
        CourseUpdatedEvent event = CourseUpdatedEvent.builder()
                .lessonId(lessonId)
                .action(CourseAction.UPDATE_LESSON_QUIZ)
                .build();
        rabbitTemplate.convertAndSend(RabbitConfig.COURSE_EXCHANGE, "", event);
    }

    public void publishRemoveCourse(Long courseId){
        CourseUpdatedEvent event = CourseUpdatedEvent.builder()
                .courseId(courseId)
                .action(CourseAction.REMOVE_COURSE)
                .build();
        rabbitTemplate.convertAndSend(RabbitConfig.COURSE_EXCHANGE, "", event);
    }
}