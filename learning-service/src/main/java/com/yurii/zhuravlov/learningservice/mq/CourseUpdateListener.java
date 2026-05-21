package com.yurii.zhuravlov.learningservice.mq;

import com.yurii.zhuravlov.eventsDto.CourseUpdatedEvent;
import com.yurii.zhuravlov.learningservice.config.RabbitConfig;
import com.yurii.zhuravlov.learningservice.repo.EnrolmentRepository;
import com.yurii.zhuravlov.learningservice.repo.UserLessonProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseUpdateListener {
    private final EnrolmentRepository enrolmentRepository;
    private final UserLessonProgressRepository userLessonProgressRepository;

    @RabbitListener(queues = RabbitConfig.COURSE_QUEUE)
    @Transactional
    @CacheEvict(value = "user-enrollments", allEntries = true)
    public void handleCourseUpdate(CourseUpdatedEvent event) {
        switch (event.action()){
            case ADD_LESSON: addLesson(event); break;
            case REMOVE_COURSE: removeCourse(event); break;
            case REMOVE_LESSON: removeLesson(event); break;
            case UPDATE_LESSON_QUIZ: updateLessonQuiz(event); break;
        }
    }

    private void addLesson(CourseUpdatedEvent event){
        enrolmentRepository.addLessonAndUpdateStatus(event.courseId(), event.newTotalLessons());
    }

    private void removeLesson(CourseUpdatedEvent event){
        enrolmentRepository.updateTotalLessons(event.courseId(), event.newTotalLessons());
        userLessonProgressRepository.deleteByLessonId(event.lessonId());
    }

    private void updateLessonQuiz(CourseUpdatedEvent event){
        userLessonProgressRepository.deleteByLessonId(event.lessonId());
        enrolmentRepository.updateStatusWithUpdates(event.courseId());
    }

    private void removeCourse(CourseUpdatedEvent event){
        enrolmentRepository.deleteByCourseId(event.courseId());
    }
}