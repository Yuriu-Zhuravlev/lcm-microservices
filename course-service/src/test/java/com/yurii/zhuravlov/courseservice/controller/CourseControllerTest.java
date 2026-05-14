package com.yurii.zhuravlov.courseservice.controller;

import com.yurii.zhuravlov.courseservice.config.security.jwt.JwtService;
import com.yurii.zhuravlov.courseservice.handler.GlobalExceptionHandler;
import com.yurii.zhuravlov.courseservice.exception.CourseNotFoundException;
import com.yurii.zhuravlov.courseservice.service.CourseService;
import com.yurii.zhuravlov.requests.CourseRequest;
import com.yurii.zhuravlov.responses.CourseResponseShort;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {
        CourseController.class,
        GlobalExceptionHandler.class,
        TestConfig.class
})
class CourseControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CourseService courseService;

    @Autowired
    private ObjectMapper objectMapper;

    private String asJsonString(Object obj) {
        return objectMapper.writeValueAsString(obj);
    }

    @Test
    void createCourse_Success() throws Exception {
        CourseRequest request = new CourseRequest("Java Pro", "Advanced course");
        CourseResponseShort response = CourseResponseShort.builder()
                .id(1L).title("Java Pro").description("Advanced course").build();

        when(courseService.createCourse(any(CourseRequest.class), anyLong()))
                .thenReturn(response);

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Java Pro"));
    }

    @Test
    void createCourse_ValidationFailed_ShouldReturn400() throws Exception {
        CourseRequest invalidRequest = new CourseRequest("J", "Desc");

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getCourseById_NotFound_ShouldReturn404() throws Exception {
        when(courseService.getCourseById(99L))
                .thenThrow(new CourseNotFoundException());

        mockMvc.perform(get("/api/courses/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Course service: exception occurred"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getCourseById_InternalServerError() throws Exception {
        when(courseService.getCourseById(99L))
                .thenThrow(new RuntimeException());

        mockMvc.perform(get("/api/courses/99"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    void deleteCourse_Success() throws Exception {
        mockMvc.perform(delete("/api/courses/1"))
                .andExpect(status().isNoContent());

        verify(courseService).deleteCourse(eq(1L), any());
    }

    @Test
    void getAllCourses_ShouldReturnList() throws Exception {
        when(courseService.getAll()).thenReturn(List.of(
                CourseResponseShort.builder().id(1L).title("Course 1").build(),
                CourseResponseShort.builder().id(2L).title("Course 2").build()
        ));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Course 1"));
    }

    @Test
    void getAllCoursesByIds_ShouldReturnList() throws Exception {
        when(courseService.findByIds(anyList())).thenReturn(List.of(
                CourseResponseShort.builder().id(1L).title("Course 1").build(),
                CourseResponseShort.builder().id(2L).title("Course 2").build()
        ));

        mockMvc.perform(get("/api/courses/byIds")
                    .param("ids", "1,2")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));
    }

    @Test
    void getMyCourses_ShouldReturnAuthorCourses() throws Exception {
        when(courseService.getCoursesByAuthor(1L))
                .thenReturn(List.of(CourseResponseShort.builder().id(10L).title("My Course").build()));

        mockMvc.perform(get("/api/courses/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L))
                .andExpect(jsonPath("$[0].title").value("My Course"));
    }

    @Test
    void getCourseShortById_ShouldReturnShortDto() throws Exception {
        Long id = 1L;
        CourseResponseShort response = CourseResponseShort.builder().id(id).title("Short View").build();
        when(courseService.getCourseShortById(id)).thenReturn(response);

        mockMvc.perform(get("/api/courses/short/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Short View"));
    }

    @Test
    void updateCourse_Success() throws Exception {
        Long courseId = 1L;
        CourseRequest request = new CourseRequest("Updated Title", "Updated Desc");
        CourseResponseShort response = CourseResponseShort.builder()
                .id(courseId)
                .title("Updated Title")
                .build();

        when(courseService.updateCourse(eq(courseId), any(CourseRequest.class), eq(1L)))
                .thenReturn(response);

        mockMvc.perform(put("/api/courses/{id}", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

}