package com.smartgrade.smartgrade_backend.repository;

import com.smartgrade.smartgrade_backend.entity.QuestionEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {

    @EntityGraph(attributePaths = {"answers"})
    List<QuestionEntity> findAllByQuiz_PkOrderByPositionAsc(Long quizPk);
}

