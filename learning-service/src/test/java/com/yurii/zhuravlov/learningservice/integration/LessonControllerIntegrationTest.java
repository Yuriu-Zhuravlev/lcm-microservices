package com.yurii.zhuravlov.learningservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yurii.zhuravlov.learningservice.model.Enrolment;
import com.yurii.zhuravlov.learningservice.model.enums.EnrolmentStatus;
import com.yurii.zhuravlov.responses.LessonResponseFull;
import com.yurii.zhuravlov.responses.QuizCorrectAnswersResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class LessonControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    protected ObjectMapper objectMapper;

    private static final Long USER_ID = 1L;
    private static final Long COURSE_ID = 10L;
    private static final Long LESSON_ID = 55L;

    private void saveEnrolment() {
        enrolmentRepository.save(Enrolment.builder()
                .userId(USER_ID).courseId(COURSE_ID)
                .status(EnrolmentStatus.ENROLLED)
                .enrolledAt(LocalDateTime.now())
                .totalLessonsCount(1).build());
    }

    private LessonResponseFull mockLesson() {
        return LessonResponseFull.builder()
                .id(LESSON_ID).title("Lesson").htmlContent("<p>content</p>")
                .courseId(COURSE_ID)
                .orderIndex(0)
                .questions(List.of()).build();
    }

    @Test
    void getLessonById_WithoutToken_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/learning/lesson/{id}", LESSON_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    void getLessonById_Enrolled_ShouldReturn200() throws Exception {
        saveEnrolment();
        when(courseServiceClient.getLessonByIdInternal(LESSON_ID)).thenReturn(mockLesson());

        mockMvc.perform(get("/api/learning/lesson/{id}", LESSON_ID)
                        .header("Authorization", "Bearer " + generateToken(USER_ID, "yurii")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(LESSON_ID));
    }

    @Test
    void getLessonById_NotEnrolled_ShouldReturn403() throws Exception {
        when(courseServiceClient.getLessonByIdInternal(LESSON_ID)).thenReturn(mockLesson());

        mockMvc.perform(get("/api/learning/lesson/{id}", LESSON_ID)
                        .header("Authorization", "Bearer " + generateToken(USER_ID, "yurii")))
                .andExpect(status().isForbidden());
    }

    @Test
    void submitQuiz_Enrolled_NoQuestions_ShouldCompleteLesson() throws Exception {
        saveEnrolment();
        when(courseServiceClient.getCorrectAnswers(LESSON_ID)).thenReturn(
                QuizCorrectAnswersResponse.builder()
                        .lessonId(LESSON_ID).courseId(COURSE_ID).answers(Map.of()).build());

        mockMvc.perform(post("/api/learning/lesson/{id}/submit", LESSON_ID)
                        .header("Authorization", "Bearer " + generateToken(USER_ID, "yurii"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCompleted").value(true))
                .andExpect(jsonPath("$.scorePercentage").value(100.0));
    }

    @Test
    void submitQuiz_NotEnrolled_ShouldReturn403() throws Exception {
        when(courseServiceClient.getCorrectAnswers(LESSON_ID)).thenReturn(
                QuizCorrectAnswersResponse.builder()
                        .lessonId(LESSON_ID).courseId(COURSE_ID).answers(Map.of()).build());

        mockMvc.perform(post("/api/learning/lesson/{id}/submit", LESSON_ID)
                        .header("Authorization", "Bearer " + generateToken(USER_ID, "yurii"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void submitQuiz_WithAnswers_ShouldCalculateScore() throws Exception {
        saveEnrolment();
        when(courseServiceClient.getCorrectAnswers(LESSON_ID)).thenReturn(
                QuizCorrectAnswersResponse.builder()
                        .lessonId(LESSON_ID).courseId(COURSE_ID)
                        .answers(Map.of(1L, 'A', 2L, 'B', 3L, 'C', 4L, 'D'))
                        .build());

        Map<Long,Character> request = Map.of(1L, 'A', 2L, 'B', 3L, 'C', 4L, 'X');

        mockMvc.perform(post("/api/learning/lesson/{id}/submit", LESSON_ID)
                        .header("Authorization", "Bearer " + generateToken(USER_ID, "yurii"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCompleted").value(false))
                .andExpect(jsonPath("$.correctAnswers").value(3))
                .andExpect(jsonPath("$.totalQuestions").value(4))
                .andExpect(jsonPath("$.scorePercentage").value(75.0));
    }
}