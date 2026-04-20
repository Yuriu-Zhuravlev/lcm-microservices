package com.yurii.zhuravlov.learningservice.repo;

import com.yurii.zhuravlov.learningservice.model.Enrolment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnrolmentRepository extends JpaRepository<Enrolment, Long> {
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    List<Enrolment> findByUserId(Long userId);
}
