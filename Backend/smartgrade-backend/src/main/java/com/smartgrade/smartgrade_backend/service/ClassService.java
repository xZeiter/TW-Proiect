package com.smartgrade.smartgrade_backend.service;

import com.smartgrade.smartgrade_backend.dto.classroom.ClassDto;
import com.smartgrade.smartgrade_backend.entity.ClassEntity;
import com.smartgrade.smartgrade_backend.entity.UserEntity;
import com.smartgrade.smartgrade_backend.repository.ClassRepository;
import com.smartgrade.smartgrade_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClassService {

    private final ClassRepository classRepository;
    private final UserRepository userRepository;

    public ClassService(ClassRepository classRepository, UserRepository userRepository) {
        this.classRepository = classRepository;
        this.userRepository = userRepository;
    }

    public List<ClassDto> getAll(String ownerEmail) {
        return classRepository.findAllByOwner_EmailOrderByPkAsc(ownerEmail)
                .stream()
                .map(e -> new ClassDto(e.getId(), e.getName()))
                .toList();
    }

    public ClassDto create(String ownerEmail, ClassDto dto) {
        if (dto.getId() == null || dto.getId().isBlank()) throw new RuntimeException("Class id is required");
        if (dto.getName() == null || dto.getName().isBlank()) throw new RuntimeException("Class name is required");

        UserEntity owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Owner user not found"));

        if (classRepository.existsByIdAndOwner_Email(dto.getId(), ownerEmail)) {
            throw new RuntimeException("Class already exists");
        }

        ClassEntity saved = classRepository.save(new ClassEntity(dto.getId(), dto.getName(), owner));
        return new ClassDto(saved.getId(), saved.getName());
    }

    public ClassDto update(String ownerEmail, String id, ClassDto dto) {
        ClassEntity existing = classRepository.findByIdAndOwner_Email(id, ownerEmail)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        if (dto.getName() != null && !dto.getName().isBlank()) {
            existing.setName(dto.getName());
        }

        ClassEntity saved = classRepository.save(existing);
        return new ClassDto(saved.getId(), saved.getName());
    }

    public void delete(String ownerEmail, String id) {
        ClassEntity existing = classRepository.findByIdAndOwner_Email(id, ownerEmail)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        classRepository.delete(existing);
    }
}
