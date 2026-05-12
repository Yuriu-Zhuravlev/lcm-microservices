package com.yurii.zhuravlov.courseservice.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class QuestionTest {
    @Test
    void setOptionsList_ShouldCorrectlyMapOptions() {
        Question question = new Question();
        List<Option> input = List.of(
                new Option("Option 1", false),
                new Option("Option 2", true),
                new Option("Option 3", false)
        );

        question.setOptionsList(input);

        Map<Character, Option> options = question.getOptions();
        assertThat(options).hasSize(3);
        assertThat(options.get('A').getText()).isEqualTo("Option 1");
        assertThat(options.get('B').isCorrect()).isTrue();
        assertThat(options.get('C').getText()).isEqualTo("Option 3");
    }

    @Test
    void setOptionsList_WhenNoCorrectAnswer_ShouldThrowException() {
        Question question = new Question();
        List<Option> input = List.of(
                new Option("False 1", false),
                new Option("False 2", false)
        );

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> question.setOptionsList(input));

        assertThat(exception.getMessage()).isEqualTo("Question must have exactly one correct answer");
    }

    @Test
    void setOptionsList_WhenMultipleCorrectAnswers_ShouldThrowException() {
        Question question = new Question();
        List<Option> input = List.of(
                new Option("True 1", true),
                new Option("True 2", true)
        );

        assertThrows(IllegalArgumentException.class, () -> question.setOptionsList(input));
    }

    @Test
    void setOptionsList_WhenTooFewOptions_ShouldThrowException() {
        Question question = new Question();
        List<Option> input = List.of(new Option("Only one", true));

        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> question.setOptionsList(input));

        assertThat(exception.getMessage()).isEqualTo("Question should have at least 2 variants");
    }
}