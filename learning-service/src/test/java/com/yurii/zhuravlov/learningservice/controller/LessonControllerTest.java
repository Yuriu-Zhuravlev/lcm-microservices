package com.yurii.zhuravlov.learningservice.controller;

import com.yurii.zhuravlov.learningservice.config.security.jwt.JwtService;
import com.yurii.zhuravlov.learningservice.service.LessonService;
import com.yurii.zhuravlov.requests.QuizSubmitRequest;
import com.yurii.zhuravlov.responses.LessonResponseFull;
import com.yurii.zhuravlov.responses.QuizSubmitResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LessonController.class)
@AutoConfigureMockMvc(addFilters = false)
class LessonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LessonService lessonService;

    @MockitoBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    void getLessonById_ShouldReturnResponse() throws Exception {
        when(lessonService.getLessonContent(anyLong(),any())).thenReturn(
                LessonResponseFull.builder()
                        .id(1L)
                        .title("L1")
                        .htmlContent("...")
                        .courseId(2L)
                        .build()
        );

        mockMvc.perform(get("/api/learning/lesson/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("L1"));
    }

    @Test
    void submitQuiz_ShouldReturnResponse() throws Exception {
        when(lessonService.submitLessonProgress(any(),any(QuizSubmitRequest.class))).thenReturn(
                QuizSubmitResponse.builder()
                        .isCompleted(true)
                        .totalQuestions(6)
                        .correctAnswers(5)
                        .scorePercentage(83.33)
                        .build()
        );

        Map<Long, Character> answers = Map.of(
                101L, 'A',
                102L, 'C',
                103L, 'A',
                104L, 'B',
                105L, 'A',
                106L, 'D'
        );

        mockMvc.perform(post("/api/learning/lesson/100/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(answers)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCompleted").value(true))
                .andExpect(jsonPath("$.scorePercentage").value(83.33));
    }

    @Test
    void submitQuiz_ShouldReturnRuntimeException() throws Exception {
        when(lessonService.submitLessonProgress(any(),any(QuizSubmitRequest.class))).thenThrow(new RuntimeException());

        Map<Long, Character> answers = Map.of(
                101L, 'A',
                102L, 'C',
                103L, 'A',
                104L, 'B',
                105L, 'A',
                106L, 'D'
        );

        mockMvc.perform(post("/api/learning/lesson/100/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(answers)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.status").value(500));
    }
}