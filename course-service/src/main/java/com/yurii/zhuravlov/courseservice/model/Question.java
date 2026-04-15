package com.yurii.zhuravlov.courseservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "questions", schema = "courses_schema")
@Getter
@Setter
@NoArgsConstructor
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String text;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @ElementCollection
    @CollectionTable(
            name = "question_options",
            schema = "courses_schema",
            joinColumns = @JoinColumn(name = "question_id")
    )
    @MapKeyColumn(name = "option_letter")
    private Map<Character, Option> options = new HashMap<>();

    public void setOptionsList(List<Option> inputOptions) {
        if (inputOptions == null || inputOptions.size() < 2) {
            throw new IllegalArgumentException("Question should have at least 2 variants");
        }

        long correctCount = inputOptions.stream().filter(Option::isCorrect).count();
        if (correctCount != 1) {
            throw new IllegalArgumentException("Question must have exactly one correct answer");
        }

        this.options.clear();
        char letter = 'A';
        for (Option opt : inputOptions) {
            this.options.put(letter++, opt);
        }
    }
}