package com.smartgrade.smartgrade_backend.repository;

import com.smartgrade.smartgrade_backend.entity.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<StudentEntity, Long> {

    List<StudentEntity> findAllByOwner_EmailOrderByPkAsc(String email);

    Optional<StudentEntity> findByIdAndOwner_Email(String id, String email);

    boolean existsByIdAndOwner_Email(String id, String email);

    boolean existsByExternalIdAndOwner_Email(String externalId, String email);

    // needed for scan pipeline (map extId -> student)
    Optional<StudentEntity> findByExternalIdAndOwner_Email(String externalId, String email);

    // NEW: resolve studentPk safely (pk is Long)
    Optional<StudentEntity> findByPkAndOwner_Email(Long pk, String email);
}
