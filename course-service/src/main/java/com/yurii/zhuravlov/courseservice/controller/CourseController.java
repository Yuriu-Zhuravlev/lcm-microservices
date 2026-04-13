package com.yurii.zhuravlov.courseservice.controller;

import com.yurii.zhuravlov.courseservice.client.AuthClient;
import com.yurii.zhuravlov.courseservice.model.Course;
import com.yurii.zhuravlov.courseservice.service.CourseService;
import com.yurii.zhuravlov.requests.CourseRequest;
import com.yurii.zhuravlov.responses.CourseResponse;
import com.yurii.zhuravlov.responses.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final AuthClient authClient;

    @GetMapping
    public List<CourseResponse> getAllCourses() {
        return courseService.getAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(
            @Valid @RequestBody CourseRequest courseRequest
    ){
        Long userId = (Long) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getDetails();
        Course course = courseService.createCourse(courseRequest.title(), courseRequest.description(), userId);
        CourseResponse response = mapToResponse(course);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public List<CourseResponse> getMyCourses() {
        Long userId = (Long) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getDetails();
        return courseService.getCoursesByAuthor(userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public CourseResponse getCourseById(@PathVariable Long id){
        return mapToResponse(courseService.getCourseById(id));
    }

    @PutMapping("/{id}")
    public CourseResponse updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseRequest request) {
        Long userId = (Long) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getDetails();
        return mapToResponse(courseService.updateCourse(id, request.title(), request.description(), userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        Long userId = (Long) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getDetails();
        courseService.deleteCourse(id, userId);
        return ResponseEntity.noContent().build();
    }

    private CourseResponse mapToResponse(Course course) {
        UserResponse user;
        try {
            user = authClient.getUserById(course.getAuthorId());
        } catch (Exception e) {
            user = new UserResponse(course.getAuthorId(), "Unknown");
        }
        return new CourseResponse(course.getId(), course.getTitle(), course.getDescription(), user);
    }
}