package com.yurii.zhuravlov.learningservice.repo;

import com.yurii.zhuravlov.learningservice.model.UserLessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface UserLessonProgressRepository extends JpaRepository<UserLessonProgress, Long> {
    Optional<UserLessonProgress> findByEnrolmentIdAndLessonId(Long enrollmentId, Long lessonId);

    @Modifying
    @Transactional
    void deleteByLessonId(Long lessonId);
}
