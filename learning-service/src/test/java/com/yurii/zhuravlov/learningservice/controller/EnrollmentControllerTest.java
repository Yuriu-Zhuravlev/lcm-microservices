package com.yurii.zhuravlov.learningservice.controller;

import com.yurii.zhuravlov.learningservice.config.security.jwt.JwtService;
import com.yurii.zhuravlov.learningservice.exceptions.EnrollmentNotFoundException;
import com.yurii.zhuravlov.learningservice.service.EnrollmentService;
import com.yurii.zhuravlov.responses.EnrollmentResponse;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(EnrollmentController.class)
// Якщо у тебе є Security конфігурація, може знадобитися вимкнути її для цього тесту:
@AutoConfigureMockMvc(addFilters = false)
class EnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnrollmentService enrollmentService;

    @MockitoBean
    private JwtService jwtService;

    // Допоміжний об'єкт для тестів
    private EnrollmentResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = EnrollmentResponse.builder()
                .id(1L)
                .enrollmentStatus("ENROLLED")
                .build();
    }

    @Test
    void enroll_ShouldReturnCreated() throws Exception {
        Long courseId = 100L;

        when(enrollmentService.enrollUser(any(), anyLong())).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/learning/enrollment/{courseId}", courseId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.enrollmentStatus").value("ENROLLED"));
    }

    @Test
    void getMyEnrollments_ShouldReturnList() throws Exception {
        when(enrollmentService.getEnrollmentsByUserId(any())).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/learning/enrollment/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1));
    }

    // --- ТЕСТУВАННЯ EXCEPTION HANDLER ---

    @Test
    void getEnrollmentById_WhenNotFound_ShouldReturn404FromHandler() throws Exception {
        // Змушуємо сервіс викинути кастомний ексепшн
        when(enrollmentService.getEnrollmentById(any(), eq(999L)))
                .thenThrow(new EnrollmentNotFoundException());

        mockMvc.perform(get("/api/learning/enrollment/{id}", 999L))
                .andExpect(status().isNotFound()) // Перевіряємо, що статус 404
                .andExpect(jsonPath("$.message").value("Enrollment not found")) // Текст із твого Exception
                .andExpect(jsonPath("$.status").value(404)); // Перевіряємо структуру ErrorResponse
    }

    @Test
    void completeCourse_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(post("/api/learning/enrollment/complete/{enrolmentId}", 1L))
                .andExpect(status().isNoContent());

        verify(enrollmentService).tryCompleteCourse(any(), eq(1L));
    }

    @Test
    void handleFeignException_ShouldReturnExternalServiceError() throws Exception {
        // Імітуємо помилку від Feign (наприклад, курс не знайдено в іншому сервісі)
        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(503);
        when(feignException.getMessage()).thenReturn("Service Unavailable");

        when(enrollmentService.getEnrollmentById(any(), anyLong()))
                .thenThrow(feignException);

        mockMvc.perform(get("/api/learning/enrollment/1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("External Service Error"));
    }

    @Test
    void handleFeignException_ShouldReturnInternalServiceError() throws Exception {
        // Імітуємо помилку від Feign (наприклад, курс не знайдено в іншому сервісі)
        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(-1);
        when(feignException.getMessage()).thenReturn("Service Unavailable");

        when(enrollmentService.getEnrollmentById(any(), anyLong()))
                .thenThrow(feignException);

        mockMvc.perform(get("/api/learning/enrollment/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("External Service Error"));
    }
}