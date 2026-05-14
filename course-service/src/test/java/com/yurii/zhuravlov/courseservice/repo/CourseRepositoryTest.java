package com.yurii.zhuravlov.courseservice.repo;

import com.yurii.zhuravlov.courseservice.model.Course;
import com.yurii.zhuravlov.courseservice.model.Lesson;
import com.yurii.zhuravlov.courseservice.model.Option;
import com.yurii.zhuravlov.courseservice.model.Question;
import org.hibernate.query.NativeQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CourseRepositoryTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByAuthorId_ShouldReturnCoursesForSpecificAuthor() {
        Long authorId = 1L;
        Long otherAuthorId = 2L;

        Course course1 = Course.builder().title("Java").authorId(authorId).build();
        Course course2 = Course.builder().title("Spring").authorId(authorId).build();
        Course course3 = Course.builder().title("Python").authorId(otherAuthorId).build();

        entityManager.persist(course1);
        entityManager.persist(course2);
        entityManager.persist(course3);
        entityManager.flush();

        List<Course> result = courseRepository.findByAuthorId(authorId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Course::getTitle).containsExactlyInAnyOrder("Java", "Spring");
        assertThat(result).allMatch(c -> c.getAuthorId().equals(authorId));
    }

    @Test
    void findByAuthorId_WhenNoCourses_ShouldReturnEmptyList() {
        List<Course> result = courseRepository.findByAuthorId(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void saveCourse_ShouldPersistWithSchema() {
        Course course = Course.builder()
                .title("Database Course")
                .authorId(10L)
                .description("Long description")
                .build();

        Course saved = courseRepository.save(course);

        assertThat(saved.getId()).isNotNull();
        Course found = entityManager.find(Course.class, saved.getId());
        assert found != null;
        assertThat(found.getTitle()).isEqualTo("Database Course");
    }

    @Test
    void deleteCourse_ShouldCascadeDeleteEverything() {
        Course course = Course.builder()
                .title("Java Masterclass")
                .authorId(1L)
                .lessons(new ArrayList<>())
                .build();

        Lesson lesson = Lesson.builder()
                .title("Hibernate Basics")
                .course(course)
                .questions(new ArrayList<>())
                .build();
        course.getLessons().add(lesson);

        Question question = new Question();
        question.setText("What is JPA?");
        question.setLesson(lesson);
        question.setOptionsList(List.of(
                new Option("Java Persistence API", true),
                new Option("Just Plain Arrays", false)
        ));
        lesson.getQuestions().add(question);

        Course savedCourse = courseRepository.saveAndFlush(course);
        Long courseId = savedCourse.getId();
        Long lessonId = savedCourse.getLessons().getFirst().getId();
        Long questionId = savedCourse.getLessons().getFirst().getQuestions().getFirst().getId();

        assertThat(entityManager.find(Course.class, courseId)).isNotNull();
        assertThat(entityManager.find(Lesson.class, lessonId)).isNotNull();
        assertThat(entityManager.find(Question.class, questionId)).isNotNull();

        courseRepository.deleteById(courseId);
        courseRepository.flush();
        entityManager.clear();

        assertThat(entityManager.find(Course.class, courseId)).isNull();
        assertThat(entityManager.find(Lesson.class, lessonId)).isNull();
        assertThat(entityManager.find(Question.class, questionId)).isNull();

        NativeQuery query = (NativeQuery) entityManager.getEntityManager()
                .createNativeQuery("SELECT count(*) FROM courses_schema.question_options WHERE question_id = ?1");
        query.setParameter(1, questionId);
        assertThat(((Number) query.getSingleResult()).intValue()).isEqualTo(0);
    }
}