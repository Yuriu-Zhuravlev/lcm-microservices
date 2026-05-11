package com.yurii.zhuravlov.courseservice.repo;

import com.yurii.zhuravlov.courseservice.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    @Query("SELECT q FROM Question q JOIN FETCH q.lesson l JOIN FETCH l.course c WHERE q.id = :id")
    Optional<Question> findByIdWithCourse(@Param("id") Long id);
}
