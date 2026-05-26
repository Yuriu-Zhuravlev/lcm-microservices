package com.yurii.zhuravlov.courseservice.integration;

import com.yurii.zhuravlov.courseservice.model.Course;
import com.yurii.zhuravlov.courseservice.model.Lesson;
import com.yurii.zhuravlov.requests.LessonCreteRequest;
import com.yurii.zhuravlov.requests.LessonUpdateRequest;
import com.yurii.zhuravlov.responses.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class LessonControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String token;
    private Course course;
    private Lesson lesson;

    @BeforeEach
    void setUpData() {
        token = generateToken(1L, "yurii");
        course = courseRepository.save(Course.builder()
                .title("Course").description("Desc").authorId(1L).build());
        lesson = lessonRepository.save(Lesson.builder()
                .title("Lesson 1").htmlContent("<p>content</p>")
                .course(course).orderIndex(1).build());
        when(authClient.getUserById(1L)).thenReturn(new UserResponse(1L, "Yurii"));
    }

    @Test
    void getLessonById_WithoutToken_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/courses/lessons/{id}", lesson.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getLessonByIdInternal_WithoutInternalHeader_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/courses/lessons/internal/{id}", lesson.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void getLessonByIdInternal_WithWrongHeader_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/courses/lessons/internal/{id}", lesson.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("X-Internal-Service", "hacker-service"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getLessonByIdInternal_WithCorrectHeader_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/courses/lessons/internal/{id}", lesson.getId())
                        .header("X-Internal-Service", "learning-service")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Lesson 1"));
    }

    @Test
    void getLessonById_WithToken_ShouldReturnLesson() throws Exception {
        mockMvc.perform(get("/api/courses/lessons/{id}", lesson.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Lesson 1"));
    }

    @Test
    void getLessonById_NotAuthor_ShouldReturn403() throws Exception {
        Course otherCourse = courseRepository.save(Course.builder()
                .title("Other").description("Desc").authorId(99L).build());
        Lesson otherLesson = lessonRepository.save(Lesson.builder()
                .title("Other Lesson").htmlContent("<p>x</p>")
                .course(otherCourse).orderIndex(1).build());

        mockMvc.perform(get("/api/courses/lessons/{id}", otherLesson.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void createLesson_WithToken_ShouldReturn201() throws Exception {
        LessonCreteRequest lessonCreteRequest = new LessonCreteRequest("New Lesson","<p>html</p>", 2, course.getId());

        mockMvc.perform(post("/api/courses/lessons")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lessonCreteRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New Lesson"));
    }

    @Test
    void createLesson_ValidationFailed_ShouldReturn400() throws Exception {
        LessonCreteRequest lessonCreteRequest = new LessonCreteRequest("","", 2, null);


        mockMvc.perform(post("/api/courses/lessons")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lessonCreteRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateLesson_WithToken_ShouldReturnUpdated() throws Exception {
        LessonUpdateRequest lessonUpdateRequest = new LessonUpdateRequest("Updated Lesson", "<p>new</p>", 1);

        mockMvc.perform(put("/api/courses/lessons/{id}", lesson.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lessonUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Lesson"));
    }

    @Test
    void updateLesson_NotAuthor_ShouldReturn403() throws Exception {
        Course otherCourse = courseRepository.save(Course.builder()
                .title("Other").description("Desc").authorId(99L).build());
        Lesson otherLesson = lessonRepository.save(Lesson.builder()
                .title("Other").htmlContent("<p>x</p>")
                .course(otherCourse).orderIndex(1).build());

        LessonUpdateRequest lessonUpdateRequest = new LessonUpdateRequest("Hack", "<p>hack</p>", 1);

        mockMvc.perform(put("/api/courses/lessons/{id}", otherLesson.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lessonUpdateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteLesson_WithToken_ShouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/courses/lessons/{id}", lesson.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteLesson_NotAuthor_ShouldReturn403() throws Exception {
        Course otherCourse = courseRepository.save(Course.builder()
                .title("Other").description("Desc").authorId(99L).build());
        Lesson otherLesson = lessonRepository.save(Lesson.builder()
                .title("Other").htmlContent("<p>x</p>")
                .course(otherCourse).orderIndex(1).build());

        mockMvc.perform(delete("/api/courses/lessons/{id}", otherLesson.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}