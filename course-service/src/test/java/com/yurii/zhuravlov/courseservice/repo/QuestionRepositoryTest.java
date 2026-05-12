package com.yurii.zhuravlov.courseservice.repo;

import com.yurii.zhuravlov.courseservice.model.Course;
import com.yurii.zhuravlov.courseservice.model.Lesson;
import com.yurii.zhuravlov.courseservice.model.Option;
import com.yurii.zhuravlov.courseservice.model.Question;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class QuestionRepositoryTest {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByIdWithCourse_ShouldFetchFullHierarchy() {
        // Given
        Course course = Course.builder().title("Cloud Native").authorId(1L).build();
        entityManager.persist(course);

        Lesson lesson = Lesson.builder().title("Docker").course(course).build();
        entityManager.persist(lesson);

        Question question = new Question();
        question.setText("What is a container?");
        question.setLesson(lesson);
        question.setOptionsList(List.of(
                new Option("Isolated process", true),
                new Option("Virtual Machine", false)
        ));
        entityManager.persist(question);

        entityManager.flush();
        entityManager.clear();

        Optional<Question> result = questionRepository.findByIdWithCourse(question.getId());

        assertThat(result).isPresent();
        Question foundQuestion = result.get();

        assertThat(foundQuestion.getText()).isEqualTo("What is a container?");

        assertThat(foundQuestion.getLesson()).isNotNull();
        assertThat(foundQuestion.getLesson().getTitle()).isEqualTo("Docker");
        assertThat(foundQuestion.getLesson().getCourse()).isNotNull();
        assertThat(foundQuestion.getLesson().getCourse().getTitle()).isEqualTo("Cloud Native");
    }
}