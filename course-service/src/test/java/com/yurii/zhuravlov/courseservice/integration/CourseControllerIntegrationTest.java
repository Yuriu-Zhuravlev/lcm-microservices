package com.yurii.zhuravlov.courseservice.integration;

import com.yurii.zhuravlov.courseservice.model.Course;
import com.yurii.zhuravlov.requests.CourseRequest;
import com.yurii.zhuravlov.responses.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class CourseControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String token;
    private Course course;

    @BeforeEach
    void setUpData() {
        token = generateToken(1L, "yurii");
        course = courseRepository.save(Course.builder()
                .title("Spring Boot Course").description("Learn Spring Boot").authorId(1L).build());
        when(authClient.getUserById(1L)).thenReturn(new UserResponse(1L, "Yurii"));
    }

    @Test
    void getAllCourses_WithoutToken_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCourseById_WithoutToken_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/courses/{id}", course.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllCourses_WithToken_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/api/courses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Spring Boot Course"));
    }

    @Test
    void getCourseById_WithToken_ShouldReturnCourse() throws Exception {
        mockMvc.perform(get("/api/courses/{id}", course.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Spring Boot Course"))
                .andExpect(jsonPath("$.description").value("Learn Spring Boot"));
    }

    @Test
    void getCourseById_NotFound_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/courses/99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCourseShortById_WithToken_ShouldReturnShortDto() throws Exception {
        mockMvc.perform(get("/api/courses/short/{id}", course.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Spring Boot Course"));
    }

    @Test
    void getMyCourses_WithToken_ShouldReturnAuthorCourses() throws Exception {
        mockMvc.perform(get("/api/courses/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Spring Boot Course"));
    }

    @Test
    void createCourse_WithToken_ShouldReturn201() throws Exception {
        CourseRequest courseRequest = new CourseRequest("New Course", "Some description");

        mockMvc.perform(post("/api/courses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(courseRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New Course"));
    }

    @Test
    void createCourse_ValidationFailed_ShouldReturn400() throws Exception {
        CourseRequest courseRequest = new CourseRequest("A", "Desc");

        mockMvc.perform(post("/api/courses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(courseRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCourse_WithToken_ShouldReturnUpdated() throws Exception {
        CourseRequest courseRequest = new CourseRequest("Updated Title", "Updated Desc");

        mockMvc.perform(put("/api/courses/{id}", course.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(courseRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    void updateCourse_NotAuthor_ShouldReturn403() throws Exception {
        Course otherCourse = courseRepository.save(Course.builder()
                .title("Other").description("Desc").authorId(99L).build());

        CourseRequest courseRequest = new CourseRequest("Hack", "Updated Desc");

        mockMvc.perform(put("/api/courses/{id}", otherCourse.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(courseRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteCourse_WithToken_ShouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/courses/{id}", course.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(courseRepository.findById(course.getId())).isEmpty();
    }

    @Test
    void deleteCourse_NotAuthor_ShouldReturn403() throws Exception {
        Course otherCourse = courseRepository.save(Course.builder()
                .title("Other").description("Desc").authorId(99L).build());

        mockMvc.perform(delete("/api/courses/{id}", otherCourse.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}