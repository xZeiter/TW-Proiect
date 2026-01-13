package com.smartgrade.smartgrade_backend.repository;

import com.smartgrade.smartgrade_backend.entity.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassRepository extends JpaRepository<ClassEntity, Long> {

    List<ClassEntity> findAllByOwner_EmailOrderByPkAsc(String email);

    Optional<ClassEntity> findByIdAndOwner_Email(String id, String email);

    boolean existsByIdAndOwner_Email(String id, String email);
}
