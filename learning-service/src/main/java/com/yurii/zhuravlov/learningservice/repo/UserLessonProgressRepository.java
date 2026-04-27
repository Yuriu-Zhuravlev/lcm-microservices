package com.yurii.zhuravlov.learningservice.repo;

import com.yurii.zhuravlov.learningservice.model.UserLessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserLessonProgressRepository extends JpaRepository<UserLessonProgress, Long> {
    Optional<UserLessonProgress> findByEnrolmentIdAndLessonId(Long enrollmentId, Long lessonId);
}
