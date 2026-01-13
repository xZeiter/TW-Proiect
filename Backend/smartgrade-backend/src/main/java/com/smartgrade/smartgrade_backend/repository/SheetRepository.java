package com.smartgrade.smartgrade_backend.repository;

import com.smartgrade.smartgrade_backend.entity.SheetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SheetRepository extends JpaRepository<SheetEntity, Long> {

    List<SheetEntity> findAllByQuiz_IdOrderByIdAsc(String quizId);
}
