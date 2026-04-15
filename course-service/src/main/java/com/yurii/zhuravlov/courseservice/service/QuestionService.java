package com.yurii.zhuravlov.courseservice.service;

import com.yurii.zhuravlov.courseservice.exception.LessonNotFoundException;
import com.yurii.zhuravlov.courseservice.exception.NotAnAuthorException;
import com.yurii.zhuravlov.courseservice.exception.QuestionNotFoundException;
import com.yurii.zhuravlov.courseservice.model.Lesson;
import com.yurii.zhuravlov.courseservice.model.Option;
import com.yurii.zhuravlov.courseservice.model.Question;
import com.yurii.zhuravlov.courseservice.repo.LessonRepository;
import com.yurii.zhuravlov.courseservice.repo.QuestionRepository;
import com.yurii.zhuravlov.courseservice.utils.MappingUtils;
import com.yurii.zhuravlov.requests.QuestionRequest;
import com.yurii.zhuravlov.responses.QuestionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final LessonRepository lessonRepository;

    @Transactional
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

        return MappingUtils.toQuestionDto(questionRepository.save(question));
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
        return MappingUtils.toQuestionDto(questionRepository.save(question));
    }

    @Transactional
    public void deleteQuestion(Long questionId, Long userId) {
        Question question = questionRepository.findByIdWithCourse(questionId)
                .orElseThrow(QuestionNotFoundException::new);

        if (!question.getLesson().getCourse().getAuthorId().equals(userId)) {
            throw new NotAnAuthorException();
        }

        questionRepository.delete(question);
    }

    public QuestionResponse getById(Long questionId){
        return MappingUtils.toQuestionDto(
                questionRepository.findById(questionId)
                        .orElseThrow(QuestionNotFoundException::new)
        );
    }

    public List<QuestionResponse> getByLessonId(Long lessonId){
        return questionRepository.findByLessonId(lessonId)
                .stream().map(MappingUtils::toQuestionDto)
                .toList();
    }
}
