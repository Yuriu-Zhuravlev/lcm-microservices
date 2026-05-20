package com.yurii.zhuravlov.courseservice.service;

import com.yurii.zhuravlov.courseservice.exception.CourseNotFoundException;
import com.yurii.zhuravlov.courseservice.exception.LessonNotFoundException;
import com.yurii.zhuravlov.courseservice.exception.NotAnAuthorException;
import com.yurii.zhuravlov.courseservice.model.Course;
import com.yurii.zhuravlov.courseservice.model.Lesson;
import com.yurii.zhuravlov.courseservice.model.Question;
import com.yurii.zhuravlov.courseservice.mq.CourseEventPublisher;
import com.yurii.zhuravlov.courseservice.repo.CourseRepository;
import com.yurii.zhuravlov.courseservice.repo.LessonRepository;
import com.yurii.zhuravlov.courseservice.utils.MappingUtils;
import com.yurii.zhuravlov.requests.LessonCreteRequest;
import com.yurii.zhuravlov.requests.LessonUpdateRequest;
import com.yurii.zhuravlov.responses.LessonResponseFull;
import com.yurii.zhuravlov.responses.QuizCorrectAnswersResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonService {
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final CourseEventPublisher courseEventPublisher;
    private final RedisTemplate<String, Object> redisTemplate;


    @Transactional
    public LessonResponseFull getLessonById(Long id, Long userId){
        Lesson lesson = lessonRepository.findById(id).orElseThrow(LessonNotFoundException::new);

        if (!lesson.getCourse().getAuthorId().equals(userId)) {
            throw new NotAnAuthorException();
        }

        return MappingUtils.toLessonFullDto(lesson, true);
    }

    @Transactional
    @Cacheable(value = "lesson-internal", key = "#id")
    public LessonResponseFull getLessonByIdInternal(Long id){
        Lesson lesson = lessonRepository.findById(id).orElseThrow(LessonNotFoundException::new);

        return MappingUtils.toLessonFullDto(lesson, false);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "courses", key = "#lessonRequest.courseId"),
            @CacheEvict(value = "courses-short", key = "#lessonRequest.courseId")
    })
    public LessonResponseFull createLesson(LessonCreteRequest lessonRequest, Long userId){
        Course course = courseRepository.findById(lessonRequest.courseId()).orElseThrow(CourseNotFoundException::new);

        if (!course.getAuthorId().equals(userId)){
            throw new NotAnAuthorException();
        }

        Lesson lesson = lessonRepository.save(Lesson.builder()
                .title(lessonRequest.title())
                .htmlContent(lessonRequest.htmlContent())
                .course(course)
                .orderIndex(lessonRequest.orderIndex())
                .build()
        );
        int newTotalLessons = lessonRepository.countByCourseId(course.getId());

        courseEventPublisher.publishAddLesson(course.getId(), newTotalLessons);

        return MappingUtils.toLessonFullDto(lesson, true);
    }

    @Transactional
    @CacheEvict(value = "lesson-internal", key = "#lessonId")
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
    @CacheEvict(value = "lesson-internal", key = "#lessonId")
    public void deleteLesson(Long lessonId, Long userId){
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(LessonNotFoundException::new);

        Long courseId = lesson.getCourse().getId();

        if (!lesson.getCourse().getAuthorId().equals(userId)) {
            throw new NotAnAuthorException();
        }

        lessonRepository.delete(lesson);

        redisTemplate.delete("courses::" + courseId);
        redisTemplate.delete("courses-short::" + courseId);
        int newTotalLessons = lessonRepository.countByCourseId(courseId);
        courseEventPublisher.publishRemoveLesson(courseId, lessonId, newTotalLessons);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "correct-answers", key = "#lessonId")
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
