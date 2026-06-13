package com.yurii.zhuravlov.learningservice.integration;

import com.yurii.zhuravlov.learningservice.model.Enrolment;
import com.yurii.zhuravlov.learningservice.model.enums.EnrolmentStatus;
import com.yurii.zhuravlov.responses.CourseResponseFull;
import com.yurii.zhuravlov.responses.CourseResponseShort;
import com.yurii.zhuravlov.responses.UserResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EnrollmentControllerIntegrationTest extends BaseIntegrationTest {

    private static final Long USER_ID = 1L;
    private static final Long COURSE_ID = 10L;

    private CourseResponseShort mockCourse() {
        return CourseResponseShort.builder()
                .id(COURSE_ID).title("Course").description("Desc").totalLessonsCount(3).build();
    }

    private Enrolment saveEnrolment() {
        return enrolmentRepository.save(Enrolment.builder()
                .userId(USER_ID).courseId(COURSE_ID)
                .status(EnrolmentStatus.ENROLLED)
                .enrolledAt(LocalDateTime.now())
                .totalLessonsCount(3).build());
    }

    @Test
    void enroll_WithoutToken_ShouldReturn403() throws Exception {
        mockMvc.perform(post("/api/learning/enrollment/{courseId}", COURSE_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyEnrollments_WithoutToken_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/learning/enrollment/my"))
                .andExpect(status().isForbidden());
    }

    @Test
    void enroll_WithToken_ShouldReturn201() throws Exception {
        when(courseServiceClient.getCourseShortById(COURSE_ID)).thenReturn(mockCourse());

        mockMvc.perform(post("/api/learning/enrollment/{courseId}", COURSE_ID)
                        .header("Authorization", "Bearer " + generateToken(USER_ID, "yurii")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.course.id").value(COURSE_ID));
    }

    @Test
    void enroll_AlreadyEnrolled_ShouldReturn400() throws Exception {
        saveEnrolment();
        when(courseServiceClient.getCourseShortById(COURSE_ID)).thenReturn(mockCourse());

        mockMvc.perform(post("/api/learning/enrollment/{courseId}", COURSE_ID)
                        .header("Authorization", "Bearer " + generateToken(USER_ID, "yurii")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMyEnrollments_WithToken_ShouldReturnList() throws Exception {
        saveEnrolment();
        when(courseServiceClient.getAllCoursesByIds(anyList())).thenReturn(List.of(mockCourse()));

        mockMvc.perform(get("/api/learning/enrollment/my")
                        .header("Authorization", "Bearer " + generateToken(USER_ID, "yurii")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrollmentResponses").isArray())
                .andExpect(jsonPath("$.enrollmentResponses[0].course.id").value(COURSE_ID));
    }

    @Test
    void getEnrollmentById_WithToken_ShouldReturnEnrollment() throws Exception {
        Enrolment enrolment = saveEnrolment();
        when(courseServiceClient.getCourseById(COURSE_ID)).thenReturn(
                CourseResponseFull.builder()
                        .id(COURSE_ID).title("Course").description("Desc")
                        .author(new UserResponse(1L, "Yurii"))
                        .lessons(List.of()).build());

        mockMvc.perform(get("/api/learning/enrollment/{id}", enrolment.getId())
                        .header("Authorization", "Bearer " + generateToken(USER_ID, "yurii")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(enrolment.getId()));
    }

    @Test
    void getEnrollmentById_NotYours_ShouldReturn403() throws Exception {
        Enrolment enrolment = saveEnrolment();

        mockMvc.perform(get("/api/learning/enrollment/{id}", enrolment.getId())
                        .header("Authorization", "Bearer " + generateToken(99L, "hacker")))
                .andExpect(status().isForbidden());
    }

    @Test
    void completeCourse_AllLessonsCompleted_ShouldReturn204() throws Exception {
        Enrolment enrolment = enrolmentRepository.save(Enrolment.builder()
                .userId(USER_ID).courseId(COURSE_ID)
                .status(EnrolmentStatus.ENROLLED)
                .enrolledAt(LocalDateTime.now())
                .totalLessonsCount(0).build());

        mockMvc.perform(post("/api/learning/enrollment/complete/{id}", enrolment.getId())
                        .header("Authorization", "Bearer " + generateToken(USER_ID, "yurii")))
                .andExpect(status().isNoContent());
    }

    @Test
    void completeCourse_NotAllCompleted_ShouldReturn409() throws Exception {
        Enrolment enrolment = enrolmentRepository.save(Enrolment.builder()
                .userId(USER_ID).courseId(COURSE_ID)
                .status(EnrolmentStatus.ENROLLED)
                .enrolledAt(LocalDateTime.now())
                .totalLessonsCount(5).build());

        mockMvc.perform(post("/api/learning/enrollment/complete/{id}", enrolment.getId())
                        .header("Authorization", "Bearer " + generateToken(USER_ID, "yurii")))
                .andExpect(status().isBadRequest());
    }
}