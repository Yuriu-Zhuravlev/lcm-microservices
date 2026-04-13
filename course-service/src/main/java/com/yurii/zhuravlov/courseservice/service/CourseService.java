package com.yurii.zhuravlov.courseservice.service;

import com.yurii.zhuravlov.courseservice.exception.CourseNotFoundException;
import com.yurii.zhuravlov.courseservice.exception.NotAnAuthor;
import com.yurii.zhuravlov.courseservice.model.Course;
import com.yurii.zhuravlov.courseservice.repo.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository repository;

    public Course createCourse(String title, String description, Long authorId) {
        return repository.save(Course.builder()
                .title(title)
                .description(description)
                .authorId(authorId)
                .build());
    }

    public List<Course> getAll() {
        return repository.findAll();
    }

    public Course getCourseById(Long id){
        return repository.findById(id).orElseThrow(CourseNotFoundException::new);
    }

    public List<Course> getCoursesByAuthor(Long authorId) {
        return repository.findByAuthorId(authorId);
    }

    @Transactional
    public Course updateCourse(Long courseId, String title, String description, Long authorId) {
        Course course = repository.findById(courseId)
                .orElseThrow(CourseNotFoundException::new);

        // Перевірка власності
        if (!course.getAuthorId().equals(authorId)) {
            throw new NotAnAuthor();
        }

        course.setTitle(title);
        course.setDescription(description);
        return repository.save(course);
    }

    @Transactional
    public void deleteCourse(Long courseId, Long authorId) {
        Course course = repository.findById(courseId)
                .orElseThrow(CourseNotFoundException::new);

        if (!course.getAuthorId().equals(authorId)) {
            throw new NotAnAuthor();
        }

        repository.delete(course);
    }
}