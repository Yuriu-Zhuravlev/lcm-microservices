package com.yurii.zhuravlov.learningservice.repo;

import com.yurii.zhuravlov.learningservice.model.Enrolment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnrolmentRepository extends JpaRepository<Enrolment, Long> {
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);
}
