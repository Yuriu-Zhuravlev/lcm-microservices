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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository repository;
    private final AuthClient authClient;
    private final CourseEventPublisher courseEventPublisher;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String COURSES_SHORT_NO_LESSONS_KEY = "courses-short-no-lessons::";


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
    @Cacheable(value = "courses", key = "#id")
    public CourseResponseFull getCourseById(Long id){
        return repository.findById(id)
                .map(course -> {
                    UserResponse userResponse = getUserResponse(course.getAuthorId());
                    return MappingUtils.toCourseFullDTO(course, userResponse);
                })
                .orElseThrow(CourseNotFoundException::new);
    }

    @Transactional
    @Cacheable(value = "courses-short", key = "#id")
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
        UserResponse userResponse = getUserResponse(authorId);
        return courses.stream().map(course -> MappingUtils.toCourseShortDTO(course, userResponse)).toList();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "courses", key = "#courseId"),
            @CacheEvict(value = "courses-short", key = "#courseId"),
            @CacheEvict(value = "courses-short-no-lessons", key = "#courseId"),
    })
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
    @Caching(evict = {
            @CacheEvict(value = "courses", key = "#courseId"),
            @CacheEvict(value = "courses-short", key = "#courseId"),
            @CacheEvict(value = "courses-short-no-lessons", key = "#courseId"),
    })
    public void deleteCourse(Long courseId, Long authorId) {
        Course course = repository.findById(courseId)
                .orElseThrow(CourseNotFoundException::new);

        if (!course.getAuthorId().equals(authorId)) {
            throw new NotAnAuthorException();
        }
        if (!CollectionUtils.isEmpty(course.getLessons())) {
            course.getLessons().forEach(lesson -> {
                redisTemplate.delete("lesson-internal::" + lesson.getId());
                redisTemplate.delete("correct-answers::" + lesson.getId());
            });
        }

        repository.delete(course);

        courseEventPublisher.publishRemoveCourse(courseId);
    }

    public List<CourseResponseShort> findByIds(List<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) return List.of();

        List<String> keys = courseIds.stream()
                .map(id -> COURSES_SHORT_NO_LESSONS_KEY + id)
                .toList();

        List<Object> cached = redisTemplate.opsForValue().multiGet(keys);

        System.out.println("cached: " + cached);
        System.out.println("cached types: " + cached.stream()
                .map(v -> v == null ? "null" : v.getClass().getName())
                .toList());

        List<CourseResponseShort> result = new ArrayList<>();
        List<Long> missingIds = new ArrayList<>();

        for (int i = 0; i < courseIds.size(); i++) {
            Object value = cached.get(i);
            if (value != null) {
                result.add((CourseResponseShort) value);
            } else {
                missingIds.add(courseIds.get(i));
            }
        }

        if (!missingIds.isEmpty()) {
            List<Course> courses = repository.findAllById(missingIds);
            Set<UserResponse> userResponses = authClient.getUsersByIds(
                    courses.stream().map(Course::getAuthorId).collect(Collectors.toSet()));
            Map<Long, UserResponse> userMap = userResponses.stream()
                    .collect(Collectors.toMap(UserResponse::id, Function.identity()));

            Map<String, CourseResponseShort> toCache = new HashMap<>();
            for (Course course : courses) {
                UserResponse userResponse = userMap.getOrDefault(
                        course.getAuthorId(), new UserResponse(course.getAuthorId(), "Unknown"));
                CourseResponseShort dto = MappingUtils.toCourseShortDTO(course, userResponse);
                toCache.put(COURSES_SHORT_NO_LESSONS_KEY + course.getId(), dto);
                result.add(dto);
            }

            System.out.println("missingIds: " + missingIds);
            System.out.println("toCache keys: "+ toCache.keySet());

            redisTemplate.opsForValue().multiSet(toCache);
            toCache.keySet().forEach(key -> {
                Boolean expired = redisTemplate.expire(key, Duration.ofMinutes(60));
                System.out.println("expire " + key + " -> "+ expired);
                Object val = redisTemplate.opsForValue().get(key);
                System.out.println("get after set "+ key+ " -> "+  val);
            });
        }

        return result;
    }
}