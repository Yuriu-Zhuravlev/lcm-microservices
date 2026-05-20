package com.yurii.zhuravlov.courseservice.service;

import com.yurii.zhuravlov.courseservice.exception.LessonNotFoundException;
import com.yurii.zhuravlov.courseservice.exception.NotAnAuthorException;
import com.yurii.zhuravlov.courseservice.exception.QuestionNotFoundException;
import com.yurii.zhuravlov.courseservice.model.Lesson;
import com.yurii.zhuravlov.courseservice.model.Option;
import com.yurii.zhuravlov.courseservice.model.Question;
import com.yurii.zhuravlov.courseservice.mq.CourseEventPublisher;
import com.yurii.zhuravlov.courseservice.repo.LessonRepository;
import com.yurii.zhuravlov.courseservice.repo.QuestionRepository;
import com.yurii.zhuravlov.courseservice.utils.MappingUtils;
import com.yurii.zhuravlov.requests.QuestionRequest;
import com.yurii.zhuravlov.responses.QuestionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final LessonRepository lessonRepository;
    private final CourseEventPublisher courseEventPublisher;
    private final RedisTemplate<String, Object> redisTemplate;


    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "lesson-internal", key = "#lessonId"),
            @CacheEvict(value = "correct-answers", key = "#lessonId")
    })
    public QuestionResponse createQuestion (QuestionRequest request, Long lessonId, Long userId){
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow(LessonNotFoundException::new);
        if (!lesson.getCourse().getAuthorId().equals(userId)) {
            throw new NotAnAuthorException();
        }

        Question question = new Question();
        question.setText(request.text());
        question.setLesson(lesson);
        question.setOptionsList(
                request.options().stream()
                        .map(optionRequest ->
                                        new Option(optionRequest.text(), optionRequest.isCorrect())
                        )
                        .toList()
        );

        question = questionRepository.save(question);

        courseEventPublisher.publishUpdateQuiz(lessonId, lesson.getCourse().getId());

        return MappingUtils.toQuestionDto(question, true);
    }


    @Transactional
    public QuestionResponse updateQuestion (QuestionRequest request, Long questionId, Long userId){
        Question question = questionRepository.findByIdWithCourse(questionId)
                .orElseThrow(QuestionNotFoundException::new);
        if (!question.getLesson().getCourse().getAuthorId().equals(userId)) {
            throw new NotAnAuthorException();
        }
        question.setText(request.text());
        question.setOptionsList(
                request.options().stream()
                        .map(optionRequest ->
                                new Option(optionRequest.text(), optionRequest.isCorrect())
                        )
                        .toList()
        );

        Long lessonId = question.getLesson().getId();
        Long courseId = question.getLesson().getCourse().getId();

        question = questionRepository.save(question);

        redisTemplate.delete("lesson-internal::" + lessonId);
        redisTemplate.delete("correct-answers::" + lessonId);

        courseEventPublisher.publishUpdateQuiz(lessonId, courseId);

        return MappingUtils.toQuestionDto(question, true);
    }

    @Transactional
    public void deleteQuestion(Long questionId, Long userId) {
        Question question = questionRepository.findByIdWithCourse(questionId)
                .orElseThrow(QuestionNotFoundException::new);

        if (!question.getLesson().getCourse().getAuthorId().equals(userId)) {
            throw new NotAnAuthorException();
        }

        questionRepository.delete(question);

        Long lessonId = question.getLesson().getId();
        Long courseId = question.getLesson().getCourse().getId();

        redisTemplate.delete("lesson-internal::" + lessonId);
        redisTemplate.delete("correct-answers::" + lessonId);

        courseEventPublisher.publishUpdateQuiz(lessonId, courseId);
    }
}
