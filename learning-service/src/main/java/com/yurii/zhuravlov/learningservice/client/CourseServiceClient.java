package com.yurii.zhuravlov.learningservice.client;

import com.yurii.zhuravlov.responses.CourseResponseFull;
import com.yurii.zhuravlov.responses.CourseResponseShort;
import com.yurii.zhuravlov.responses.LessonResponseFull;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "course-service") // Назва сервісу з Eureka
public interface CourseServiceClient {

    @GetMapping("/api/courses/lessons/{id}")
    LessonResponseFull getLessonById(@PathVariable Long id);

    @GetMapping("/api/courses/{courseId}")
    CourseResponseFull getCourseById(@PathVariable Long courseId);

    @GetMapping("/api/courses/short/{courseId}")
    CourseResponseShort getCourseShortById(@PathVariable Long courseId);

    @GetMapping("/api/courses/byIds")
    List<CourseResponseShort> getAllCoursesByIds(@RequestParam List<Long> ids);
}
