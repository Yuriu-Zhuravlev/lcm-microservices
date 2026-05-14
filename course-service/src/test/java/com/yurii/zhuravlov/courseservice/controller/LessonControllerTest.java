package com.yurii.zhuravlov.courseservice.controller;

import com.yurii.zhuravlov.courseservice.config.security.jwt.JwtService;
import com.yurii.zhuravlov.courseservice.handler.GlobalExceptionHandler;
import com.yurii.zhuravlov.courseservice.service.LessonService;
import com.yurii.zhuravlov.requests.LessonCreteRequest;
import com.yurii.zhuravlov.requests.LessonUpdateRequest;
import com.yurii.zhuravlov.responses.LessonResponseFull;
import com.yurii.zhuravlov.responses.QuizCorrectAnswersResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LessonController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {
        LessonController.class,
        GlobalExceptionHandler.class,
        TestConfig.class
})
class LessonControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private LessonService lessonService;

    @Autowired
    private ObjectMapper objectMapper;

    private String asJsonString(Object obj) {
        return objectMapper.writeValueAsString(obj);
    }

    @Test
    void createLesson_Success() throws Exception {
        LessonCreteRequest request = new LessonCreteRequest("Docker Basics", "<html>...</html>", 0, 1L);
        LessonResponseFull response = LessonResponseFull.builder().id(10L).title("Docker Basics").build();

        when(lessonService.createLesson(any(LessonCreteRequest.class), eq(1L)))
                .thenReturn(response);

        mockMvc.perform(post("/api/courses/lessons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.title").value("Docker Basics"));
    }

    @Test
    void getLessonByIdInternal_Success_WithCorrectHeader() throws Exception {
        Long lessonId = 1L;
        when(lessonService.getLessonByIdInternal(lessonId))
                .thenReturn(LessonResponseFull.builder().id(lessonId).build());

        mockMvc.perform(get("/api/courses/lessons/internal/{id}", lessonId)
                        .header("X-Internal-Service", "learning-service"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lessonId));
    }

    @Test
    void getLessonByIdInternal_Forbidden_WithWrongHeader() throws Exception {
        mockMvc.perform(get("/api/courses/lessons/internal/1")
                        .header("X-Internal-Service", "hacker-service"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Only internal services allowed"));
    }

    @Test
    void getAnswersById_Success() throws Exception {
        Long lessonId = 1L;
        QuizCorrectAnswersResponse response = QuizCorrectAnswersResponse.builder()
                .lessonId(lessonId)
                .answers(Map.of(101L, 'A'))
                .build();

        when(lessonService.getCorrectAnswers(lessonId)).thenReturn(response);

        mockMvc.perform(get("/api/courses/lessons/internal/answers/{id}", lessonId)
                        .header("X-Internal-Service", "learning-service"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonId").value(lessonId))
                .andExpect(jsonPath("$.answers['101']").value("A"));
    }

    @Test
    void getAnswersById_Forbidden_WithoutHeader() throws Exception {
        Long lessonId = 1L;

        mockMvc.perform(get("/api/courses/lessons/internal/answers/{id}", lessonId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void getAnswersById_Forbidden_WithWrongHeader() throws Exception {
        Long lessonId = 1L;

        mockMvc.perform(get("/api/courses/lessons/internal/answers/{id}", lessonId)
                        .header("X-Internal-Service", "hacker-service"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Only internal services allowed"));
    }

    @Test
    void updateLesson_Success() throws Exception {
        LessonUpdateRequest request = new LessonUpdateRequest("Updated", "Content", 1);
        when(lessonService.updateLesson(eq(1L), any(), eq(1L)))
                .thenReturn(LessonResponseFull.builder().title("Updated").build());

        mockMvc.perform(put("/api/courses/lessons/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));
    }

    @Test
    void deleteLesson_Success() throws Exception {
        mockMvc.perform(delete("/api/courses/lessons/1"))
                .andExpect(status().isNoContent());

        verify(lessonService).deleteLesson(1L, 1L);
    }

    @Test
    void getLessonById_Success() throws Exception {
        Long lessonId = 1L;
        Long userId = 1L;

        LessonResponseFull response = LessonResponseFull.builder()
                .id(lessonId)
                .title("Spring MVC Testing")
                .htmlContent("<h1>Lesson Content</h1>")
                .orderIndex(1)
                .courseId(100L)
                .questions(List.of())
                .build();

        when(lessonService.getLessonById(eq(lessonId), eq(userId))).thenReturn(response);

        mockMvc.perform(get("/api/courses/lessons/{id}", lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lessonId))
                .andExpect(jsonPath("$.title").value("Spring MVC Testing"))
                .andExpect(jsonPath("$.htmlContent").value("<h1>Lesson Content</h1>"))
                .andExpect(jsonPath("$.courseId").value(100L))
                .andDo(print());

        verify(lessonService).getLessonById(lessonId, userId);
    }
}