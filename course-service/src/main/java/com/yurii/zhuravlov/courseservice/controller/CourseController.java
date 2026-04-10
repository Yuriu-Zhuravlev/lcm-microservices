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
                .map(course -> {
                            UserResponse user;
                            try {
                                user = authClient.getUserById(course.getAuthorId());
                            } catch (Exception e) {
                                user = new UserResponse(course.getAuthorId(), "Unknown");
                            }
                            return new CourseResponse(
                                    course.getId(),
                                    course.getTitle(),
                                    course.getDescription(),
                                    user
                            );
                        }
                )
                .toList();
    }

    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(@RequestHeader(value = "X-User-Id") String userId
            , @RequestBody CourseRequest courseRequest){
        Course course = courseService.createCourse(courseRequest.title(), courseRequest.description(), userId);
        UserResponse user = authClient.getUserById(course.getAuthorId());
        CourseResponse response = new CourseResponse(course.getId(), course.getTitle()
                ,course.getDescription(), user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}