package com.yurii.zhuravlov.courseservice.integration;

import com.yurii.zhuravlov.courseservice.model.Course;
import com.yurii.zhuravlov.courseservice.model.Lesson;
import com.yurii.zhuravlov.courseservice.model.Option;
import com.yurii.zhuravlov.courseservice.model.Question;
import com.yurii.zhuravlov.requests.OptionRequest;
import com.yurii.zhuravlov.requests.QuestionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class QuestionControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;


    private String token;
    private Lesson lesson;
    private Question question;

    @BeforeEach
    void setUpData() {
        token = generateToken(1L, "yurii");
        Course course = courseRepository.save(Course.builder()
                .title("Course").description("Desc").authorId(1L).build());
        lesson = lessonRepository.save(Lesson.builder()
                .title("Lesson").htmlContent("<p>html</p>")
                .course(course).orderIndex(1).build());

        Question q = new Question();
        q.setText("What is 2+2?");
        q.setLesson(lesson);
        q.setOptionsList(List.of(new Option("3", false), new Option("4", true)));
        question = questionRepository.save(q);
    }

    @Test
    void createQuestion_WithoutToken_ShouldReturn403() throws Exception {
        mockMvc.perform(post("/api/courses/questions/lesson/{lessonId}", lesson.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // POST /api/courses/questions/lesson/{lessonId}
    // -------------------------------------------------------------------------

    @Test
    void createQuestion_WithToken_ShouldReturn201() throws Exception {
        QuestionRequest questionRequest = new QuestionRequest("What is Java?",
                List.of(new OptionRequest("A language", true),
                        new OptionRequest("A car brand", false)
                )
        );

        mockMvc.perform(post("/api/courses/questions/lesson/{lessonId}", lesson.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(questionRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("What is Java?"));
    }

    @Test
    void createQuestion_InvalidRequest_ShouldReturn400() throws Exception {
        Course otherCourse = courseRepository.save(Course.builder()
                .title("Other").description("Desc").authorId(99L).build());
        Lesson otherLesson = lessonRepository.save(Lesson.builder()
                .title("Other").htmlContent("<p>x</p>")
                .course(otherCourse).orderIndex(1).build());

        QuestionRequest questionRequest = new QuestionRequest("Hack?",
                List.of(new OptionRequest("Yes", true))
        );


        mockMvc.perform(post("/api/courses/questions/lesson/{lessonId}", otherLesson.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(questionRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void createQuestion_NotAnAuthor_ShouldReturn403() throws Exception {
        Course otherCourse = courseRepository.save(Course.builder()
                .title("Other").description("Desc").authorId(99L).build());
        Lesson otherLesson = lessonRepository.save(Lesson.builder()
                .title("Other").htmlContent("<p>x</p>")
                .course(otherCourse).orderIndex(1).build());

        QuestionRequest questionRequest = new QuestionRequest("Hack?",
                List.of(new OptionRequest("Yes", true), new OptionRequest("No", false))
        );


        mockMvc.perform(post("/api/courses/questions/lesson/{lessonId}", otherLesson.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(questionRequest)))
                .andExpect(status().isForbidden());
    }


    // -------------------------------------------------------------------------
    // PUT /api/courses/questions/{id}
    // -------------------------------------------------------------------------

    @Test
    void updateQuestion_WithToken_ShouldReturnUpdated() throws Exception {
        QuestionRequest questionRequest = new QuestionRequest("Updated question?",
                List.of(new OptionRequest("Yes", true),
                        new OptionRequest("No", false)
                )
        );

        mockMvc.perform(put("/api/courses/questions/{id}", question.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(questionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Updated question?"));
    }

    @Test
    void updateQuestion_InvalidRequest_ShouldReturn400() throws Exception {
        QuestionRequest questionRequest = new QuestionRequest("Updated question?",
                List.of(new OptionRequest("Yes", true)
                )
        );

        mockMvc.perform(put("/api/courses/questions/{id}", question.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(questionRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void updateQuestion_NotAuthor_ShouldReturn403() throws Exception {
        Course otherCourse = courseRepository.save(Course.builder()
                .title("Other").description("Desc").authorId(99L).build());
        Lesson otherLesson = lessonRepository.save(Lesson.builder()
                .title("Other").htmlContent("<p>x</p>")
                .course(otherCourse).orderIndex(1).build());
        Question otherQuestion = new Question();
        otherQuestion.setText("Other?");
        otherQuestion.setLesson(otherLesson);
        otherQuestion.setOptionsList(List.of(new Option("A", true), new Option("B", false)));
        otherQuestion = questionRepository.save(otherQuestion);

        QuestionRequest questionRequest = new QuestionRequest("Hack?",
                List.of(new OptionRequest("Yes", true), new OptionRequest("No", false))
        );

        mockMvc.perform(put("/api/courses/questions/{id}", otherQuestion.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(questionRequest)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/courses/questions/{id}
    // -------------------------------------------------------------------------

    @Test
    void deleteQuestion_WithToken_ShouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/courses/questions/{id}", question.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteQuestion_NotAuthor_ShouldReturn403() throws Exception {
        Course otherCourse = courseRepository.save(Course.builder()
                .title("Other").description("Desc").authorId(99L).build());
        Lesson otherLesson = lessonRepository.save(Lesson.builder()
                .title("Other").htmlContent("<p>x</p>")
                .course(otherCourse).orderIndex(1).build());
        Question otherQuestion = new Question();
        otherQuestion.setText("Other?");
        otherQuestion.setLesson(otherLesson);
        otherQuestion.setOptionsList(List.of(new Option("A", true), new Option("B", false)));
        otherQuestion = questionRepository.save(otherQuestion);

        mockMvc.perform(delete("/api/courses/questions/{id}", otherQuestion.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}