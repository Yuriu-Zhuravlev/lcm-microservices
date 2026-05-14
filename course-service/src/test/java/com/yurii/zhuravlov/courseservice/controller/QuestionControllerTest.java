package com.yurii.zhuravlov.courseservice.controller;

import com.yurii.zhuravlov.courseservice.config.security.jwt.JwtService;
import com.yurii.zhuravlov.courseservice.handler.GlobalExceptionHandler;
import com.yurii.zhuravlov.courseservice.service.QuestionService;
import com.yurii.zhuravlov.requests.OptionRequest;
import com.yurii.zhuravlov.requests.QuestionRequest;
import com.yurii.zhuravlov.responses.OptionResponse;
import com.yurii.zhuravlov.responses.QuestionResponse;
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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuestionController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {
        QuestionController.class,
        GlobalExceptionHandler.class,
        TestConfig.class
})
class QuestionControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private QuestionService questionService;

    @Autowired
    private ObjectMapper objectMapper;

    private String asJsonString(Object obj) {
        return objectMapper.writeValueAsString(obj);
    }

    @Test
    void createQuestion_Success() throws Exception {
        Long lessonId = 1L;
        QuestionRequest request = new QuestionRequest(
                "What is the capital of France?",
                List.of(
                        new OptionRequest("Paris", true),
                        new OptionRequest("London", false)
                )
        );

        QuestionResponse response = QuestionResponse.builder()
                .id(100L)
                .text(request.text())
                .options(Map.of('A', new OptionResponse("Paris", true)))
                .build();

        when(questionService.createQuestion(any(QuestionRequest.class), eq(lessonId), eq(1L)))
                .thenReturn(response);

        mockMvc.perform(post("/api/courses/questions/lesson/{lessonId}", lessonId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.text").value("What is the capital of France?"))
                .andExpect(jsonPath("$.options.A.text").value("Paris"));
    }

    @Test
    void updateQuestion_Success() throws Exception {
        Long questionId = 1L;
        QuestionRequest request = new QuestionRequest("New Text", List.of(new OptionRequest("Opt", true), new OptionRequest("Opt2", false)));

        when(questionService.updateQuestion(any(), eq(questionId), eq(1L)))
                .thenReturn(QuestionResponse.builder().id(questionId).text("New Text").build());

        mockMvc.perform(put("/api/courses/questions/{id}", questionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("New Text"));
    }

    @Test
    void createQuestion_ValidationFailed_TooFewOptions() throws Exception {
        QuestionRequest invalidRequest = new QuestionRequest("Invalid?", List.of(new OptionRequest("Only one", true)));

        mockMvc.perform(post("/api/courses/questions/lesson/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").value(containsString("options: Question must have at least 2 options")));
    }

    @Test
    void deleteQuestion_Success() throws Exception {
        mockMvc.perform(delete("/api/courses/questions/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(questionService).deleteQuestion(1L, 1L);
    }
}