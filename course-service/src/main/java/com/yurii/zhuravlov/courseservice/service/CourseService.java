package com.yurii.zhuravlov.courseservice.service;

import com.yurii.zhuravlov.courseservice.client.AuthClient;
import com.yurii.zhuravlov.courseservice.exception.CourseNotFoundException;
import com.yurii.zhuravlov.courseservice.exception.NotAnAuthorException;
import com.yurii.zhuravlov.courseservice.model.Course;
import com.yurii.zhuravlov.courseservice.mq.CourseEventPublisher;
import com.yurii.zhuravlov.courseservice.repo.CourseRepository;
import com.yurii.zhuravlov.courseservice.utils.MappingUtils;
import com.yurii.zhuravlov.requests.CourseRequest;
import com.yurii.zhuravlov.responses.CourseResponseFull;
import com.yurii.zhuravlov.responses.CourseResponseShort;
import com.yurii.zhuravlov.responses.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository repository;
    private final AuthClient authClient;
    private final CourseEventPublisher courseEventPublisher;



    public CourseResponseShort createCourse(CourseRequest courseRequest, Long authorId) {
        Course created = repository.save(Course.builder()
                .title(courseRequest.title())
                .description(courseRequest.description())
                .authorId(authorId)
                .build());
        UserResponse user = getUserResponse(created.getAuthorId());
        return MappingUtils.toCourseShortDTO(created, user);
    }

    private UserResponse getUserResponse(Long id) {
        UserResponse user;
        try {
            user = authClient.getUserById(id);
        } catch (Exception e) {
            user = new UserResponse(id, "Unknown");
        }
        return user;
    }

    public List<CourseResponseShort> getAll() {
        List<Course> courses = repository.findAll();
        Set<UserResponse> userResponses = authClient.getUsersByIds(
                courses.stream().map(Course::getAuthorId).collect(Collectors.toSet())
        );
        Map<Long, UserResponse> userMap = userResponses.stream()
                .collect(Collectors.toMap(UserResponse::id, Function.identity()));
        return courses.stream().map(course -> {
            UserResponse userResponse = userMap.getOrDefault(course.getAuthorId(),
                    new UserResponse(course.getAuthorId(), "Unknown"));
            return MappingUtils.toCourseShortDTO(course, userResponse);
        }).toList();
    }

    @Transactional
    public CourseResponseFull getCourseById(Long id){
        return repository.findById(id)
                .map(course -> {
                    UserResponse userResponse = getUserResponse(course.getAuthorId());
                    return MappingUtils.toCourseFullDTO(course, userResponse);
                })
                .orElseThrow(CourseNotFoundException::new);
    }

    @Transactional
    public CourseResponseShort getCourseShortById(Long id){
        return repository.findById(id)
                .map(course -> {
                    UserResponse userResponse = getUserResponse(course.getAuthorId());
                    return MappingUtils.toCourseShortDTOWithLessonCount(course, userResponse);
                })
                .orElseThrow(CourseNotFoundException::new);
    }

    public List<CourseResponseShort> getCoursesByAuthor(Long authorId) {
        List<Course> courses = repository.findByAuthorId(authorId);
        Set<UserResponse> userResponses = authClient.getUsersByIds(
                courses.stream().map(Course::getAuthorId).collect(Collectors.toSet()));
        Map<Long, UserResponse> userMap = userResponses.stream()
                .collect(Collectors.toMap(UserResponse::id, Function.identity()));
        return courses.stream().map(course -> {
            UserResponse userResponse = userMap.getOrDefault(course.getAuthorId(),
                    new UserResponse(course.getAuthorId(), "Unknown"));
            return MappingUtils.toCourseShortDTO(course, userResponse);
        }).toList();
    }

    @Transactional
    public CourseResponseShort updateCourse(Long courseId, CourseRequest request, Long authorId) {
        Course course = repository.findById(courseId)
                .orElseThrow(CourseNotFoundException::new);

        // Перевірка власності
        if (!course.getAuthorId().equals(authorId)) {
            throw new NotAnAuthorException();
        }

        course.setTitle(request.title());
        course.setDescription(request.description());
        course = repository.save(course);
        UserResponse userResponse = getUserResponse(course.getAuthorId());
        return MappingUtils.toCourseShortDTO(course, userResponse);
    }

    @Transactional
    public void deleteCourse(Long courseId, Long authorId) {
        Course course = repository.findById(courseId)
                .orElseThrow(CourseNotFoundException::new);

        if (!course.getAuthorId().equals(authorId)) {
            throw new NotAnAuthorException();
        }

        repository.delete(course);

        courseEventPublisher.publishRemoveCourse(courseId);
    }

    public List<CourseResponseShort> findByIds(List<Long> courseIds){
        List<Course> courses = repository.findAllById(courseIds);
        Set<UserResponse> userResponses = authClient.getUsersByIds(
                courses.stream().map(Course::getAuthorId).collect(Collectors.toSet()));
        Map<Long, UserResponse> userMap = userResponses.stream()
                .collect(Collectors.toMap(UserResponse::id, Function.identity()));
        return courses.stream().map(course -> {
            UserResponse userResponse = userMap.getOrDefault(course.getAuthorId(),
                    new UserResponse(course.getAuthorId(), "Unknown"));
            return MappingUtils.toCourseShortDTO(course, userResponse);
        }).toList();
    }
}