package com.yurii.zhuravlov.courseservice.utils;

import com.yurii.zhuravlov.courseservice.model.Course;
import com.yurii.zhuravlov.courseservice.model.Lesson;
import com.yurii.zhuravlov.courseservice.model.Option;
import com.yurii.zhuravlov.courseservice.model.Question;
import com.yurii.zhuravlov.responses.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MappingUtilsTest {
    @Test
    void toCourseShortDTOWithLessonCount_ShouldCountLessonsCorrectly() {
        Course course = Course.builder()
                .id(1L)
                .title("Java")
                .lessons(List.of(new Lesson(), new Lesson()))
                .build();
        UserResponse user = new UserResponse(1L, "Yurii");

        CourseResponseShort result = MappingUtils.toCourseShortDTOWithLessonCount(course, user);

        assertThat(result.totalLessonsCount()).isEqualTo(2);
        assertThat(result.author().username()).isEqualTo("Yurii");
    }

    @Test
    void toCourseShortDTOWithLessonCount_WithNoLessons() {
        Course course = Course.builder()
                .id(1L)
                .title("Java")
                .build();
        UserResponse user = new UserResponse(1L, "Yurii");

        CourseResponseShort result = MappingUtils.toCourseShortDTOWithLessonCount(course, user);

        assertThat(result.totalLessonsCount()).isEqualTo(0);
        assertThat(result.author().username()).isEqualTo("Yurii");
    }

    @Test
    void toQuestionDto_WhenFullIsTrue_ShouldIncludeCorrectFlag() {
        Question question = new Question();
        question.setId(10L);
        question.setText("Capital of Ukraine?");

        Option kyiv = new Option("Kyiv", true);
        question.setOptions(Map.of('A', kyiv));

        QuestionResponse result = MappingUtils.toQuestionDto(question, true);

        assertThat(result.options().get('A').isCorrect()).isTrue();
    }

    @Test
    void toQuestionDto_WhenFullIsFalse_ShouldHideCorrectFlag() {
        Question question = new Question();
        Option kyiv = new Option("Kyiv", true);
        question.setOptions(Map.of('A', kyiv));

        QuestionResponse result = MappingUtils.toQuestionDto(question, false);

        assertThat(result.options().get('A').text()).isEqualTo("Kyiv");
        assertThat(result.options().get('A').isCorrect()).isNull();
    }

    @Test
    void toLessonFullDto_ShouldMapAllFields() {
        Course course = Course.builder().id(5L).build();
        Lesson lesson = Lesson.builder()
                .id(1L)
                .title("Intro")
                .htmlContent("<p>Hello</p>")
                .orderIndex(0)
                .course(course)
                .questions(List.of(new Question(), new Question()))
                .build();

        LessonResponseFull result = MappingUtils.toLessonFullDto(lesson, true);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.courseId()).isEqualTo(5L);
        assertThat(result.htmlContent()).isEqualTo("<p>Hello</p>");
        assertThat(result.questions().size()).isEqualTo(2);
    }

    @Test
    void toLessonFullDto_WithNoQuestions() {
        Course course = Course.builder().id(5L).build();
        Lesson lesson = Lesson.builder()
                .id(1L)
                .title("Intro")
                .htmlContent("<p>Hello</p>")
                .orderIndex(0)
                .course(course)
                .build();

        LessonResponseFull result = MappingUtils.toLessonFullDto(lesson, true);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.courseId()).isEqualTo(5L);
        assertThat(result.htmlContent()).isEqualTo("<p>Hello</p>");
        assertThat(result.questions()).isNull();
    }

    @Test
    void toCourseFullDTO_ShouldMapAllFieldsIncludingLessons() {
        UserResponse user = new UserResponse(1L, "Yurii");

        Course course = Course.builder()
                .id(100L)
                .title("Full Course")
                .description("Full Description")
                .lessons(new ArrayList<>())
                .build();

        Lesson lesson = Lesson.builder()
                .id(1L)
                .title("First Lesson")
                .orderIndex(0)
                .course(course)
                .build();
        course.getLessons().add(lesson);

        CourseResponseFull result = MappingUtils.toCourseFullDTO(course, user);

        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.title()).isEqualTo("Full Course");
        assertThat(result.author()).isEqualTo(user);

        assertThat(result.lessons()).hasSize(1);
        assertThat(result.lessons().getFirst().title()).isEqualTo("First Lesson");
        assertThat(result.lessons().getFirst().id()).isEqualTo(1L);
    }

    @Test
    void toLessonShortDto_ShouldMapBasicFields() {
        Lesson lesson = Lesson.builder()
                .id(5L)
                .title("Short Lesson")
                .orderIndex(10)
                .build();

        LessonResponseShort result = MappingUtils.toLessonShortDto(lesson);

        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.title()).isEqualTo("Short Lesson");
        assertThat(result.orderIndex()).isEqualTo(10);
    }

    @Test
    void toCourseShortDTO_ShouldMapFieldsWithoutLessonCount() {
        UserResponse user = new UserResponse(2L, "Ivan");
        Course course = Course.builder()
                .id(50L)
                .title("Short Course")
                .description("Short Description")
                .build();

        CourseResponseShort result = MappingUtils.toCourseShortDTO(course, user);

        assertThat(result.id()).isEqualTo(50L);
        assertThat(result.title()).isEqualTo("Short Course");
        assertThat(result.author()).isEqualTo(user);
        assertThat(result.totalLessonsCount()).isNull();
    }

    @Test
    void toCourseFullDTO_WhenLessonsAreNull_ShouldReturnNullLessons() {
        Course course = Course.builder().lessons(null).build();

        CourseResponseFull result = MappingUtils.toCourseFullDTO(course, null);

        assertThat(result.lessons()).isNull();
    }
}