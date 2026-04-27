package com.yurii.zhuravlov.courseservice.utils;

import com.yurii.zhuravlov.courseservice.model.Course;
import com.yurii.zhuravlov.courseservice.model.Lesson;
import com.yurii.zhuravlov.courseservice.model.Option;
import com.yurii.zhuravlov.courseservice.model.Question;
import com.yurii.zhuravlov.responses.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MappingUtils {

    public static CourseResponseShort toCourseShortDTO(Course course, UserResponse userResponse){
        return CourseResponseShort.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .author(userResponse)
                .build();
    }

    public static CourseResponseShort toCourseShortDTOWithLessonCount(Course course, UserResponse userResponse){
        return CourseResponseShort.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .author(userResponse)
                .totalLessonsCount(course.getLessons() != null ? course.getLessons().size() : 0)
                .build();
    }

    public static CourseResponseFull toCourseFullDTO(Course course, UserResponse userResponse){
        List<LessonResponseShort> lessons = course.getLessons() != null ? course.getLessons().stream()
                .map(MappingUtils::toLessonShortDto)
                .toList() : null;
        return CourseResponseFull.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .author(userResponse)
                .lessons(lessons)
                .build();
    }

    public static LessonResponseShort toLessonShortDto(Lesson lesson) {
        return LessonResponseShort.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .orderIndex(lesson.getOrderIndex())
                .build();
    }

    public static LessonResponseFull toLessonFullDto(Lesson lesson, boolean full) {
        List<QuestionResponse> questionResponses = lesson.getQuestions() != null ? lesson.getQuestions().stream()
                .map(question -> MappingUtils.toQuestionDto(question, full))
                .toList() : null;
        return LessonResponseFull.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .htmlContent(lesson.getHtmlContent())
                .orderIndex(lesson.getOrderIndex())
                .questions(questionResponses)
                .courseId(lesson.getCourse().getId())
                .build();
    }


    public static QuestionResponse toQuestionDto(Question question, boolean full) {
        return QuestionResponse.builder()
                .id(question.getId())
                .text(question.getText())
                .options(question.getOptions().entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> {
                                    if (full) {
                                        return toOptionDto(entry.getValue());
                                    } else {
                                        return toOptionShortDto(entry.getValue());
                                    }
                                })
                        ))
                .build();
    }

    private static OptionResponse toOptionDto(Option option) {
        return new OptionResponse(option.getText(), option.isCorrect());
    }

    private static OptionResponse toOptionShortDto(Option option) {
        return new OptionResponse(option.getText(), null);
    }
}
