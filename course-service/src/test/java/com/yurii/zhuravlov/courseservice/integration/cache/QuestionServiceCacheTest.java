package com.yurii.zhuravlov.courseservice.integration.cache;

import com.yurii.zhuravlov.courseservice.config.RabbitConfig;
import com.yurii.zhuravlov.courseservice.exception.LessonNotFoundException;
import com.yurii.zhuravlov.courseservice.integration.BaseIntegrationTest;
import com.yurii.zhuravlov.courseservice.model.Course;
import com.yurii.zhuravlov.courseservice.model.Lesson;
import com.yurii.zhuravlov.courseservice.model.Option;
import com.yurii.zhuravlov.courseservice.model.Question;
import com.yurii.zhuravlov.courseservice.service.LessonService;
import com.yurii.zhuravlov.courseservice.service.QuestionService;
import com.yurii.zhuravlov.eventsDto.CourseUpdatedEvent;
import com.yurii.zhuravlov.eventsDto.enums.CourseAction;
import com.yurii.zhuravlov.requests.OptionRequest;
import com.yurii.zhuravlov.requests.QuestionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class QuestionServiceCacheTest extends BaseIntegrationTest {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private LessonService lessonService;


    private Course course;
    private Lesson lesson;

    @BeforeEach
    void setUpData() {
        course = courseRepository.save(Course.builder()
                .title("Course").description("Desc").authorId(1L).build());
        lesson = lessonRepository.save(Lesson.builder()
                .title("Lesson").htmlContent("<p>html</p>")
                .course(course).orderIndex(1).build());
    }

    private QuestionRequest sampleRequest() {
        return new QuestionRequest("What is 2+2?", List.of(
                new OptionRequest("3", false),
                new OptionRequest("4", true)
        ));
    }

    private String testQueue() {
        String queue = "test.queue." + UUID.randomUUID();
        rabbitTemplate.execute(channel -> {
            channel.queueDeclare(queue, false, false, true, null);
            channel.queueBind(queue, RabbitConfig.COURSE_EXCHANGE, "");
            return null;
        });
        return queue;
    }

    private void deleteQueue(String queue) {
        rabbitTemplate.execute(channel -> {
            channel.queueDelete(queue);
            return null;
        });
    }

    @Test
    void createQuestion_ShouldEvictLessonCacheAndPublishEvent() {
        lessonService.getLessonByIdInternal(lesson.getId());

        lessonRepository.deleteAll();
        assertThat(lessonService.getLessonByIdInternal(lesson.getId())).isNotNull();

        lesson = lessonRepository.save(Lesson.builder()
                .title("Lesson").htmlContent("<p>html</p>")
                .course(course).orderIndex(1).build());
        lessonService.getLessonByIdInternal(lesson.getId());

        String queue = testQueue();

        questionService.createQuestion(sampleRequest(), lesson.getId(), 1L);

        Long lessonId = lesson.getId();
        lessonRepository.deleteAll();
        assertThatThrownBy(() -> lessonService.getLessonByIdInternal(lessonId))
                .isInstanceOf(LessonNotFoundException.class);

        Message message = rabbitTemplate.receive(queue, 3000);
        assertThat(message).isNotNull();
        CourseUpdatedEvent event = (CourseUpdatedEvent) rabbitTemplate
                .getMessageConverter().fromMessage(message);
        assertThat(event.lessonId()).isEqualTo(lesson.getId());
        assertThat(event.courseId()).isEqualTo(course.getId());
        assertThat(event.action()).isEqualTo(CourseAction.UPDATE_LESSON_QUIZ);

        deleteQueue(queue);
    }

    @Test
    void updateQuestion_ShouldEvictCacheAndPublishEvent() {
        saveQuestion();

        lessonService.getLessonByIdInternal(lesson.getId());

        lessonRepository.deleteAll();
        assertThat(lessonService.getLessonByIdInternal(lesson.getId())).isNotNull();

        lesson = lessonRepository.save(Lesson.builder()
                .title("Lesson").htmlContent("<p>html</p>")
                .course(course).orderIndex(1).build());
        Question question = questionRepository.save(buildQuestion(lesson));
        lessonService.getLessonByIdInternal(lesson.getId());

        String queue = testQueue();

        questionService.updateQuestion(sampleRequest(), question.getId(), 1L);

        Long lessonId = lesson.getId();
        lessonRepository.deleteAll();
        assertThatThrownBy(() -> lessonService.getLessonByIdInternal(lessonId))
                .isInstanceOf(LessonNotFoundException.class);

        Message message = rabbitTemplate.receive(queue, 3000);
        assertThat(message).isNotNull();
        CourseUpdatedEvent event = (CourseUpdatedEvent) rabbitTemplate
                .getMessageConverter().fromMessage(message);
        assertThat(event.lessonId()).isEqualTo(lesson.getId());
        assertThat(event.courseId()).isEqualTo(course.getId());
        assertThat(event.action()).isEqualTo(CourseAction.UPDATE_LESSON_QUIZ);

        deleteQueue(queue);
    }

    @Test
    void deleteQuestion_ShouldEvictCacheAndPublishEvent() {
        saveQuestion();

        lessonService.getLessonByIdInternal(lesson.getId());

        lessonRepository.deleteAll();
        assertThat(lessonService.getLessonByIdInternal(lesson.getId())).isNotNull();

        lesson = lessonRepository.save(Lesson.builder()
                .title("Lesson").htmlContent("<p>html</p>")
                .course(course).orderIndex(1).build());
        Question question = questionRepository.save(buildQuestion(lesson));
        lessonService.getLessonByIdInternal(lesson.getId());

        String queue = testQueue();

        questionService.deleteQuestion(question.getId(), 1L);

        Long lessonId = lesson.getId();
        lessonRepository.deleteAll();
        assertThatThrownBy(() -> lessonService.getLessonByIdInternal(lessonId))
                .isInstanceOf(LessonNotFoundException.class);

        Message message = rabbitTemplate.receive(queue, 3000);
        assertThat(message).isNotNull();
        CourseUpdatedEvent event = (CourseUpdatedEvent) rabbitTemplate
                .getMessageConverter().fromMessage(message);
        assertThat(event.lessonId()).isEqualTo(lesson.getId());
        assertThat(event.courseId()).isEqualTo(course.getId());
        assertThat(event.action()).isEqualTo(CourseAction.UPDATE_LESSON_QUIZ);

        deleteQueue(queue);
    }

    private void saveQuestion() {
        questionRepository.save(buildQuestion(lesson));
    }

    private Question buildQuestion(Lesson l) {
        Question q = new Question();
        q.setText("What is 2+2?");
        q.setLesson(l);
        q.setOptionsList(List.of(
                new Option("3", false),
                new Option("4", true)
        ));
        return q;
    }
}