package com.yurii.zhuravlov.courseservice.service;


import com.yurii.zhuravlov.courseservice.client.AuthClient;
import com.yurii.zhuravlov.courseservice.exception.NotAnAuthorException;
import com.yurii.zhuravlov.courseservice.model.Course;
import com.yurii.zhuravlov.courseservice.mq.CourseEventPublisher;
import com.yurii.zhuravlov.courseservice.repo.CourseRepository;
import com.yurii.zhuravlov.requests.CourseRequest;
import com.yurii.zhuravlov.responses.CourseResponseFull;
import com.yurii.zhuravlov.responses.CourseResponseShort;
import com.yurii.zhuravlov.responses.UserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository repository;
    @Mock
    private AuthClient authClient;
    @Mock
    private CourseEventPublisher courseEventPublisher;

    @InjectMocks
    private CourseService courseService;

    @Test
    void createCourse_ShouldSaveAndReturnDto() {
        Long authorId = 1L;
        CourseRequest request = new CourseRequest("Spring Testing", "Description");
        Course savedCourse = Course.builder()
                .id(100L).title(request.title()).description(request.description()).authorId(authorId)
                .build();
        UserResponse user = new UserResponse(authorId, "Yurii");

        when(repository.save(any(Course.class))).thenReturn(savedCourse);
        when(authClient.getUserById(authorId)).thenReturn(user);

        CourseResponseShort response = courseService.createCourse(request, authorId);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.author().username()).isEqualTo("Yurii");
        verify(repository).save(any(Course.class));
    }

    @Test
    void getUserResponse_WhenAuthClientFails_ShouldReturnUnknown() {
        Long courseId = 1L;
        Course course = Course.builder().id(courseId).authorId(99L).build();

        when(repository.findById(courseId)).thenReturn(Optional.of(course));
        when(authClient.getUserById(99L)).thenThrow(new RuntimeException("Service down"));

        CourseResponseFull response = courseService.getCourseById(courseId);

        assertThat(response.author().username()).isEqualTo("Unknown");
        assertThat(response.author().id()).isEqualTo(99L);
    }

    @Test
    void getCourseShortById() {
        Long courseId = 1L;
        Course course = Course.builder().id(courseId).authorId(99L).build();

        when(repository.findById(courseId)).thenReturn(Optional.of(course));
        when(authClient.getUserById(99L)).thenReturn(new UserResponse(99L, "U1"));

        CourseResponseShort response = courseService.getCourseShortById(courseId);

        assertThat(response.author().username()).isEqualTo("U1");
        assertThat(response.author().id()).isEqualTo(99L);
    }

    @Test
    void updateCourse_WhenNotAuthor_ShouldThrowException() {
        Long courseId = 1L;
        Long realAuthorId = 10L;
        Long hackerId = 666L;
        Course course = Course.builder().id(courseId).authorId(realAuthorId).build();

        when(repository.findById(courseId)).thenReturn(Optional.of(course));

        assertThrows(NotAnAuthorException.class,
                () -> courseService.updateCourse(courseId, new CourseRequest("Title", "Desc"), hackerId));
    }

    @Test
    void updateCourse_ShouldUpdateCorrectly() {
        Long courseId = 1L;
        Long realAuthorId = 10L;
        Course course = Course.builder().id(courseId).authorId(realAuthorId).build();
        when(authClient.getUserById(99L)).thenReturn(new UserResponse(99L, "U1"));
        when(repository.findById(courseId)).thenReturn(Optional.of(course));
        when(repository.save(any(Course.class))).thenReturn(
                Course.builder().id(courseId).authorId(realAuthorId)
                        .title("Title").description("Desc").build()
        );
        CourseResponseShort response = courseService.updateCourse(courseId, new CourseRequest("Title", "Desc"), realAuthorId);
        verify(repository).save(any(Course.class));
        assertThat(response.description()).isEqualTo("Desc");
        assertThat(response.title()).isEqualTo("Title");
    }

    @Test
    void deleteCourse_ShouldDeleteAndPublishEvent() {
        Long courseId = 1L;
        Long authorId = 10L;
        Course course = Course.builder().id(courseId).authorId(authorId).build();

        when(repository.findById(courseId)).thenReturn(Optional.of(course));

        courseService.deleteCourse(courseId, authorId);

        verify(repository).delete(course);
        verify(courseEventPublisher).publishRemoveCourse(courseId);
    }

    @Test
    void deleteCourse_NotAnAuthor() {
        Long courseId = 1L;
        Long authorId = 10L;
        Long hackerId = 666L;

        Course course = Course.builder().id(courseId).authorId(authorId).build();

        when(repository.findById(courseId)).thenReturn(Optional.of(course));

        assertThrows(NotAnAuthorException.class,
                () -> courseService.deleteCourse(courseId, hackerId));

        verify(repository, never()).delete(course);
        verify(courseEventPublisher, never()).publishRemoveCourse(courseId);
    }

    @Test
    void getAll_ShouldHandleBulkUserMapping() {
        Course c1 = Course.builder().id(1L).authorId(1L).build();
        Course c2 = Course.builder().id(2L).authorId(2L).build();

        when(repository.findAll()).thenReturn(List.of(c1,c2));
        when(authClient.getUsersByIds(any())).thenReturn(Set.of(new UserResponse(1L, "Yurii")));

        List<CourseResponseShort> result = courseService.getAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).author().username()).isEqualTo("Yurii");
        assertThat(result.get(1).author().username()).isEqualTo("Unknown");
    }

    @Test
    void getCoursesByAuthor_ShouldHandleBulkUserMapping() {
        Course c1 = Course.builder().id(1L).authorId(1L).build();
        Course c2 = Course.builder().id(2L).authorId(1L).build();

        when(repository.findByAuthorId(1L)).thenReturn(List.of(c1,c2));
        when(authClient.getUsersByIds(any())).thenReturn(Set.of(new UserResponse(1L, "Yurii")));

        List<CourseResponseShort> result = courseService.getCoursesByAuthor(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).author().username()).isEqualTo("Yurii");
        assertThat(result.get(1).author().username()).isEqualTo("Yurii");
    }

    @Test
    void findByIds_ShouldHandleBulkUserMapping() {
        Course c1 = Course.builder().id(1L).authorId(1L).build();
        Course c2 = Course.builder().id(2L).authorId(2L).build();

        when(repository.findAllById(anyIterable())).thenReturn(List.of(c1,c2));
        when(authClient.getUsersByIds(any())).thenReturn(Set.of(new UserResponse(1L, "Yurii")));

        List<CourseResponseShort> result = courseService.findByIds(List.of(1L,2L));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).author().username()).isEqualTo("Yurii");
        assertThat(result.get(1).author().username()).isEqualTo("Unknown");
    }
}