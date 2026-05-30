package com.yurii.zhuravlov.courseservice.integration.cache;

import com.yurii.zhuravlov.courseservice.config.RabbitConfig;
import com.yurii.zhuravlov.courseservice.exception.CourseNotFoundException;
import com.yurii.zhuravlov.courseservice.integration.BaseIntegrationTest;
import com.yurii.zhuravlov.courseservice.model.Course;
import com.yurii.zhuravlov.courseservice.service.CourseService;
import com.yurii.zhuravlov.eventsDto.CourseUpdatedEvent;
import com.yurii.zhuravlov.eventsDto.enums.CourseAction;
import com.yurii.zhuravlov.requests.CourseRequest;
import com.yurii.zhuravlov.responses.CourseResponseFull;
import com.yurii.zhuravlov.responses.CourseResponseShort;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class CourseServiceCacheTest extends BaseIntegrationTest {

    @Autowired
    private CourseService courseService;

    @Test
    void getCourseById_ShouldCacheResult() {
        Course course = courseRepository.save(Course.builder()
                .title("Spring Boot").description("Learn SB").authorId(1L).build());

        CourseResponseFull first = courseService.getCourseById(course.getId());
        assertThat(first.title()).isEqualTo("Spring Boot");

        courseRepository.deleteAll();

        CourseResponseFull second = courseService.getCourseById(course.getId());
        assertThat(second.title()).isEqualTo("Spring Boot");
    }

    @Test
    void getCourseShortById_ShouldCacheResult() {
        Course course = courseRepository.save(Course.builder()
                .title("Docker").description("Learn Docker").authorId(1L).build());

        CourseResponseShort response = assertDoesNotThrow(() ->
                courseService.getCourseShortById(course.getId())
        );
        assertThat(response).isNotNull();

        courseRepository.deleteAll();

        CourseResponseShort cached = assertDoesNotThrow(() ->
                courseService.getCourseShortById(course.getId())
        );
        assertThat(cached).isNotNull();
        assertThat(cached.title()).isEqualTo("Docker");
    }

    @Test
    void updateCourse_ShouldEvictCache() {
        Course course = courseRepository.save(Course.builder()
                .title("Old Title").description("Desc").authorId(1L).build());

        courseService.getCourseById(course.getId());

        courseRepository.deleteAll();
        assertThat(courseService.getCourseById(course.getId())).isNotNull();

        Course restored = courseRepository.save(Course.builder()
                .title("Old Title").description("Desc").authorId(1L).build());
        courseService.getCourseById(restored.getId());

        courseService.updateCourse(restored.getId(), new CourseRequest("New Title", "Desc"), 1L);

        courseRepository.deleteAll();
        Long id = restored.getId();
        assertThatThrownBy(() -> courseService.getCourseById(id))
                .isInstanceOf(CourseNotFoundException.class);
    }

    @Test
    void deleteCourse_ShouldEvictCacheAndPublishEvent() {
        Course course = courseRepository.save(Course.builder()
                .title("To Delete").description("Desc").authorId(1L).build());

        courseService.getCourseById(course.getId());

        courseRepository.deleteAll();
        assertThat(courseService.getCourseById(course.getId())).isNotNull();

        Course restored = courseRepository.save(Course.builder()
                .title("To Delete").description("Desc").authorId(1L).build());
        courseService.getCourseById(restored.getId());

        String testQueue = "test.queue." + UUID.randomUUID();
        rabbitTemplate.execute(channel -> {
            channel.queueDeclare(testQueue, false, false, true, null);
            channel.queueBind(testQueue, RabbitConfig.COURSE_EXCHANGE, "");
            return null;
        });

        Long id = restored.getId();
        courseService.deleteCourse(id, 1L);

        assertThatThrownBy(() -> courseService.getCourseById(id))
                .isInstanceOf(CourseNotFoundException.class);

        Message message = rabbitTemplate.receive(testQueue, 3000);
        assertThat(message).isNotNull();

        CourseUpdatedEvent event = (CourseUpdatedEvent) rabbitTemplate
                .getMessageConverter().fromMessage(message);
        assertThat(event.courseId()).isEqualTo(id);
        assertThat(event.action()).isEqualTo(CourseAction.REMOVE_COURSE);

        rabbitTemplate.execute(channel -> {
            channel.queueDelete(testQueue);
            return null;
        });
    }

    @Test
    void findByIds_ShouldCacheAndReturnFromCache() {
        Course c1 = courseRepository.save(Course.builder()
                .title("C1").description("D1").authorId(1L).build());
        Course c2 = courseRepository.save(Course.builder()
                .title("C2").description("D2").authorId(2L).build());

        List<CourseResponseShort> first = courseService.findByIds(List.of(c1.getId(), c2.getId()));
        assertThat(first).hasSize(2);

        assertThat(redisTemplate.opsForValue().get("courses-short-no-lessons::" + c1.getId())).isNotNull();
        assertThat(redisTemplate.opsForValue().get("courses-short-no-lessons::" + c2.getId())).isNotNull();

        courseRepository.deleteAll();

        List<CourseResponseShort> second = courseService.findByIds(List.of(c1.getId(), c2.getId()));
        assertThat(second).hasSize(2);
        assertThat(second.stream().map(CourseResponseShort::title).toList())
                .containsExactlyInAnyOrder("C1", "C2");
    }
}