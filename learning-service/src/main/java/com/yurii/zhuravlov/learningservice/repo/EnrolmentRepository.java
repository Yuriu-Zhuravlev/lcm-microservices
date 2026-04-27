package com.yurii.zhuravlov.learningservice.repo;

import com.yurii.zhuravlov.learningservice.model.Enrolment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrolmentRepository extends JpaRepository<Enrolment, Long> {
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    Optional<Enrolment> findByUserIdAndCourseId(Long userId, Long courseId);

    List<Enrolment> findByUserId(Long userId);

    @Query("SELECT COUNT(p) FROM UserLessonProgress p WHERE p.enrolment.id = :enrolmentId AND p.isCompleted = true")
    Long countCompletedLessons(Long enrolmentId);
}
