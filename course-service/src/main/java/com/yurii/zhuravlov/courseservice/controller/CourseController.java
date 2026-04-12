package com.yurii.zhuravlov.courseservice.controller;

import com.yurii.zhuravlov.courseservice.client.AuthClient;
import com.yurii.zhuravlov.courseservice.model.Course;
import com.yurii.zhuravlov.courseservice.service.CourseService;
import com.yurii.zhuravlov.requests.CourseRequest;
import com.yurii.zhuravlov.responses.CourseResponse;
import com.yurii.zhuravlov.responses.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<CourseResponse> createCourse(@RequestHeader(value = "X-User-Id") String userId
            , @RequestBody CourseRequest courseRequest){
        Course course = courseService.createCourse(courseRequest.title(), courseRequest.description(), userId);
        CourseResponse response = mapToResponse(course);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public List<CourseResponse> getMyCourses(@RequestHeader("X-User-Id") Long userId) {
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
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody CourseRequest request) {
        return mapToResponse(courseService.updateCourse(id, request.title(), request.description(), userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
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