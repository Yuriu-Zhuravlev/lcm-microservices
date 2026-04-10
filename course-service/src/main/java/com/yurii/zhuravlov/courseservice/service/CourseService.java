package com.yurii.zhuravlov.courseservice.service;

import com.yurii.zhuravlov.courseservice.model.Course;
import com.yurii.zhuravlov.courseservice.repo.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository repository;

    public Course createCourse(String title, String description, String authorId) {
        return repository.save(Course.builder()
                .title(title)
                .description(description)
                .authorId(Long.parseLong(authorId))
                .build());
    }

    public List<Course> getAll() {
        return repository.findAll();
    }
}