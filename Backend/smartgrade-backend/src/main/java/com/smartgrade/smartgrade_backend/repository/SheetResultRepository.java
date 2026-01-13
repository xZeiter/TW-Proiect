package com.smartgrade.smartgrade_backend.repository;

import com.smartgrade.smartgrade_backend.entity.SheetResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SheetResultRepository extends JpaRepository<SheetResultEntity, Long> {

    Optional<SheetResultEntity> findBySheetId(Long sheetId);
    
    // --- METODA NOUA ---
    List<SheetResultEntity> findAllByQuizId(String quizId);
}