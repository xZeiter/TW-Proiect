package com.smartgrade.smartgrade_backend.repository;

import com.smartgrade.smartgrade_backend.entity.QuizLayoutEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuizLayoutRepository extends JpaRepository<QuizLayoutEntity, Long> {

    Optional<QuizLayoutEntity> findByQuiz_PkAndVersion(Long quizPk, Integer version);

    Optional<QuizLayoutEntity> findTopByQuiz_PkOrderByVersionDesc(Long quizPk);

    boolean existsByQuiz_PkAndVersion(Long quizPk, Integer version);

    Optional<QuizLayoutEntity> findByQuiz_PkAndLayoutHash(Long quizPk, String layoutHash);
}
