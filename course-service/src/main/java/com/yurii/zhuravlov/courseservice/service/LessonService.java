package com.yurii.zhuravlov.courseservice.service;

import com.yurii.zhuravlov.courseservice.exception.CourseNotFoundException;
import com.yurii.zhuravlov.courseservice.exception.LessonNotFoundException;
import com.yurii.zhuravlov.courseservice.exception.NotAnAuthorException;
import com.yurii.zhuravlov.courseservice.model.Course;
import com.yurii.zhuravlov.courseservice.model.Lesson;
import com.yurii.zhuravlov.courseservice.model.Question;
import com.yurii.zhuravlov.courseservice.repo.CourseRepository;
import com.yurii.zhuravlov.courseservice.repo.LessonRepository;
import com.yurii.zhuravlov.courseservice.utils.MappingUtils;
import com.yurii.zhuravlov.requests.LessonCreteRequest;
import com.yurii.zhuravlov.requests.LessonUpdateRequest;
import com.yurii.zhuravlov.responses.LessonResponseFull;
import com.yurii.zhuravlov.responses.QuizCorrectAnswersResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonService {
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public LessonResponseFull getLessonById(Long id, Long userId){
        Lesson lesson = lessonRepository.findById(id).orElseThrow(LessonNotFoundException::new);

        if (!lesson.getCourse().getAuthorId().equals(userId)) {
            throw new NotAnAuthorException();
        }

        return MappingUtils.toLessonFullDto(lesson, true);
    }

    @Transactional
    public LessonResponseFull getLessonByIdInternal(Long id){
        Lesson lesson = lessonRepository.findById(id).orElseThrow(LessonNotFoundException::new);

        return MappingUtils.toLessonFullDto(lesson, false);
    }

    @Transactional
    public LessonResponseFull createLesson(LessonCreteRequest lessonRequest, Long userId){
        Course course = courseRepository.findById(lessonRequest.courseId()).orElseThrow(CourseNotFoundException::new);

        if (!course.getAuthorId().equals(userId)){
            throw new NotAnAuthorException();
        }

        return MappingUtils.toLessonFullDto(lessonRepository.save(Lesson.builder()
                .title(lessonRequest.title())
                .htmlContent(lessonRequest.htmlContent())
                .course(course)
                .orderIndex(lessonRequest.orderIndex())
                .build()
        ), true);
    }

    @Transactional
    public LessonResponseFull updateLesson(Long lessonId, LessonUpdateRequest request, Long userId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(LessonNotFoundException::new);

        if (!lesson.getCourse().getAuthorId().equals(userId)) {
            throw new NotAnAuthorException();
        }

        lesson.setTitle(request.title());
        lesson.setHtmlContent(request.htmlContent());
        lesson.setOrderIndex(request.orderIndex());

        lesson = lessonRepository.save(lesson);

        return MappingUtils.toLessonFullDto(lesson, true);
    }

    @Transactional
    public void deleteLesson(Long lessonId, Long userId){
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(LessonNotFoundException::new);

        if (!lesson.getCourse().getAuthorId().equals(userId)) {
            throw new NotAnAuthorException();
        }

        lessonRepository.delete(lesson);
    }

    @Transactional(readOnly = true)
    public QuizCorrectAnswersResponse getCorrectAnswers(Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new LessonNotFoundException("Lesson not found"));

        Map<Long, Character> correctAnswers = lesson.getQuestions().stream()
                .collect(Collectors.toMap(
                        Question::getId,
                        question -> question.getOptions().entrySet().stream()
                                .filter(entry -> entry.getValue().isCorrect())
                                .map(Map.Entry::getKey)
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException(
                                        "Question " + question.getId() + " has no correct option"))
                ));

        return QuizCorrectAnswersResponse.builder()
                .lessonId(lessonId)
                .courseId(lesson.getCourse().getId())
                .answers(correctAnswers)
                .build();
    }
}
