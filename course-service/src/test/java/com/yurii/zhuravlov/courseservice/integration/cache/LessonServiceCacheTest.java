package com.yurii.zhuravlov.courseservice.integration.cache;

import com.yurii.zhuravlov.courseservice.config.RabbitConfig;
import com.yurii.zhuravlov.courseservice.exception.LessonNotFoundException;
import com.yurii.zhuravlov.courseservice.integration.BaseIntegrationTest;
import com.yurii.zhuravlov.courseservice.model.Course;
import com.yurii.zhuravlov.courseservice.model.Lesson;
import com.yurii.zhuravlov.courseservice.service.LessonService;
import com.yurii.zhuravlov.eventsDto.CourseUpdatedEvent;
import com.yurii.zhuravlov.eventsDto.enums.CourseAction;
import com.yurii.zhuravlov.requests.LessonCreteRequest;
import com.yurii.zhuravlov.requests.LessonUpdateRequest;
import com.yurii.zhuravlov.responses.LessonResponseFull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class LessonServiceCacheTest extends BaseIntegrationTest {

    @Autowired
    private LessonService lessonService;

    private Course course;

    @BeforeEach
    void setUpCourse() {
        course = courseRepository.save(Course.builder()
                .title("Test Course").description("Desc").authorId(1L).build());
    }

    @Test
    void getLessonByIdInternal_ShouldCacheResult() {
        Lesson lesson = lessonRepository.save(Lesson.builder()
                .title("Lesson 1").htmlContent("<p>content</p>")
                .course(course).orderIndex(1).build());

        LessonResponseFull first = lessonService.getLessonByIdInternal(lesson.getId());
        assertThat(first.title()).isEqualTo("Lesson 1");

        lessonRepository.deleteAll();

        LessonResponseFull cached = lessonService.getLessonByIdInternal(lesson.getId());
        assertThat(cached.title()).isEqualTo("Lesson 1");
    }

    @Test
    void updateLesson_ShouldEvictLessonInternalCache() {
        Lesson lesson = lessonRepository.save(Lesson.builder()
                .title("Old").htmlContent("<p>old</p>")
                .course(course).orderIndex(1).build());

        lessonService.getLessonByIdInternal(lesson.getId());

        lessonRepository.deleteAll();
        assertThat(lessonService.getLessonByIdInternal(lesson.getId())).isNotNull();

        Lesson restored = lessonRepository.save(Lesson.builder()
                .title("Old").htmlContent("<p>old</p>")
                .course(course).orderIndex(1).build());
        lessonService.getLessonByIdInternal(restored.getId());

        lessonService.updateLesson(restored.getId(),
                new LessonUpdateRequest("New Title", "<p>new</p>", 1), 1L);

        lessonRepository.deleteAll();
        Long id = restored.getId();
        assertThatThrownBy(() -> lessonService.getLessonByIdInternal(id))
                .isInstanceOf(LessonNotFoundException.class);
    }

    @Test
    void createLesson_ShouldEvictCourseCacheAndPublishEvent() {
        String testQueue = "test.queue." + UUID.randomUUID();
        rabbitTemplate.execute(channel -> {
            channel.queueDeclare(testQueue, false, false, true, null);
            channel.queueBind(testQueue, RabbitConfig.COURSE_EXCHANGE, "");
            return null;
        });

        lessonService.createLesson(
                new LessonCreteRequest("New Lesson", "<p>html</p>", 0, course.getId()), 1L);

        Message message = rabbitTemplate.receive(testQueue, 3000);
        assertThat(message).isNotNull();

        CourseUpdatedEvent event = (CourseUpdatedEvent) rabbitTemplate
                .getMessageConverter().fromMessage(message);
        assertThat(event.courseId()).isEqualTo(course.getId());
        assertThat(event.action()).isEqualTo(CourseAction.ADD_LESSON);
        assertThat(event.newTotalLessons()).isEqualTo(1);

        rabbitTemplate.execute(channel -> {
            channel.queueDelete(testQueue);
            return null;
        });
    }

    @Test
    void deleteLesson_ShouldEvictCacheAndPublishEvent() {
        Lesson lesson = lessonRepository.save(Lesson.builder()
                .title("To Delete").htmlContent("<p>x</p>")
                .course(course).orderIndex(1).build());

        lessonService.getLessonByIdInternal(lesson.getId());

        Long lessonId = lesson.getId();
        lessonRepository.deleteAll();
        assertThat(lessonService.getLessonByIdInternal(lessonId)).isNotNull();

        Lesson restored = lessonRepository.save(Lesson.builder()
                .title("To Delete").htmlContent("<p>x</p>")
                .course(course).orderIndex(1).build());
        lessonService.getLessonByIdInternal(restored.getId());

        String testQueue = "test.queue." + UUID.randomUUID();
        rabbitTemplate.execute(channel -> {
            channel.queueDeclare(testQueue, false, false, true, null);
            channel.queueBind(testQueue, RabbitConfig.COURSE_EXCHANGE, "");
            return null;
        });

        Long restoredId = restored.getId();
        lessonService.deleteLesson(restoredId, 1L);

        assertThatThrownBy(() -> lessonService.getLessonByIdInternal(restoredId))
                .isInstanceOf(LessonNotFoundException.class);

        Message message = rabbitTemplate.receive(testQueue, 3000);
        assertThat(message).isNotNull();

        CourseUpdatedEvent event = (CourseUpdatedEvent) rabbitTemplate
                .getMessageConverter().fromMessage(message);
        assertThat(event.courseId()).isEqualTo(course.getId());
        assertThat(event.lessonId()).isEqualTo(restoredId);
        assertThat(event.action()).isEqualTo(CourseAction.REMOVE_LESSON);

        rabbitTemplate.execute(channel -> {
            channel.queueDelete(testQueue);
            return null;
        });
    }
}