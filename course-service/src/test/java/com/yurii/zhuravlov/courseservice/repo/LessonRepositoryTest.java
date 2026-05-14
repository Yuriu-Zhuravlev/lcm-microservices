package com.yurii.zhuravlov.courseservice.repo;

import com.yurii.zhuravlov.courseservice.model.Course;
import com.yurii.zhuravlov.courseservice.model.Lesson;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LessonRepositoryTest {

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void countByCourseId_ShouldReturnCorrectCount() {
        Course course1 = Course.builder().title("Java").authorId(1L).build();
        Course course2 = Course.builder().title("Spring").authorId(1L).build();
        entityManager.persist(course1);
        entityManager.persist(course2);

        Lesson lesson1 = Lesson.builder().title("Intro").course(course1).orderIndex(1).build();
        Lesson lesson2 = Lesson.builder().title("Syntax").course(course1).orderIndex(2).build();
        Lesson lesson3 = Lesson.builder().title("Beans").course(course2).orderIndex(1).build();

        entityManager.persist(lesson1);
        entityManager.persist(lesson2);
        entityManager.persist(lesson3);
        entityManager.flush();

        int countForCourse1 = lessonRepository.countByCourseId(course1.getId());
        int countForCourse2 = lessonRepository.countByCourseId(course2.getId());

        assertThat(countForCourse1).isEqualTo(2);
        assertThat(countForCourse2).isEqualTo(1);
    }

    @Test
    void saveLesson_ShouldHandleLobContent() {
        String longHtml = "<html><body>" + "a".repeat(1000) + "</body></html>";
        Course course = Course.builder().title("Web").authorId(1L).build();
        entityManager.persist(course);

        Lesson lesson = Lesson.builder()
                .title("HTML Lesson")
                .htmlContent(longHtml)
                .course(course)
                .build();

        Lesson saved = lessonRepository.save(lesson);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getHtmlContent()).isEqualTo(longHtml);
    }
}