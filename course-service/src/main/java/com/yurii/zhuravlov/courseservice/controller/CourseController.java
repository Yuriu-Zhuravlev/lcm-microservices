package com.yurii.zhuravlov.courseservice.controller;

import com.yurii.zhuravlov.courseservice.annotation.CurrentUser;
import com.yurii.zhuravlov.courseservice.service.CourseService;
import com.yurii.zhuravlov.requests.CourseRequest;
import com.yurii.zhuravlov.responses.CourseResponseFull;
import com.yurii.zhuravlov.responses.CourseResponseShort;
import jakarta.validation.Valid;
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

    @GetMapping
    public List<CourseResponseShort> getAllCourses() {
        return courseService.getAll();
    }

    @GetMapping("/byIds")
    public List<CourseResponseShort> getAllCoursesByIds(@RequestParam List<Long> ids) {
        return courseService.findByIds(ids);
    }

    @PostMapping
    public ResponseEntity<CourseResponseShort> createCourse(
            @Valid @RequestBody CourseRequest courseRequest,
            @CurrentUser Long userId
    ){
        CourseResponseShort response = courseService.createCourse(courseRequest, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public List<CourseResponseShort> getMyCourses(@CurrentUser Long userId) {
        return courseService.getCoursesByAuthor(userId);
    }

    @GetMapping("/{id}")
    public CourseResponseFull getCourseById(@PathVariable Long id){
        return courseService.getCourseById(id);
    }

    @GetMapping("/short/{id}")
    public CourseResponseShort getCourseShortById(@PathVariable Long id){
        return courseService.getCourseShortById(id);
    }

    @PutMapping("/{id}")
    public CourseResponseShort updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseRequest request,
            @CurrentUser Long userId) {
        return courseService.updateCourse(id, request, userId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id, @CurrentUser Long userId) {
        courseService.deleteCourse(id, userId);
        return ResponseEntity.noContent().build();
    }

}