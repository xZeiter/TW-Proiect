package com.smartgrade.smartgrade_backend.repository;

import com.smartgrade.smartgrade_backend.entity.QuizEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<QuizEntity, Long> {

    List<QuizEntity> findAllByOwner_EmailOrderByPkAsc(String email);

    Optional<QuizEntity> findByIdAndOwner_Email(String id, String email);

    boolean existsByIdAndOwner_Email(String id, String email);

    // OPTIONAL (util daca vrei sa incarci tot quiz-ul dintr-un singur query)
    @EntityGraph(attributePaths = {"questions", "questions.answers"})
    Optional<QuizEntity> findWithQuestionsByIdAndOwner_Email(String id, String email);
}
