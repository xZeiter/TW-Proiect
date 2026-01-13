package com.smartgrade.smartgrade_backend.service;

import com.smartgrade.smartgrade_backend.dto.student.StudentDto;
import com.smartgrade.smartgrade_backend.entity.ClassEntity;
import com.smartgrade.smartgrade_backend.entity.StudentEntity;
import com.smartgrade.smartgrade_backend.entity.UserEntity;
import com.smartgrade.smartgrade_backend.repository.ClassRepository;
import com.smartgrade.smartgrade_backend.repository.StudentRepository;
import com.smartgrade.smartgrade_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;

    public StudentService(StudentRepository studentRepository, ClassRepository classRepository, UserRepository userRepository) {
        this.studentRepository = studentRepository;
        this.classRepository = classRepository;
        this.userRepository = userRepository;
    }

    public List<StudentDto> findAll(String ownerEmail) {
        return studentRepository.findAllByOwner_EmailOrderByPkAsc(ownerEmail)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public StudentDto create(String ownerEmail, StudentDto dto) {
        if (dto == null) throw new RuntimeException("Body missing");
        if (dto.getId() == null || dto.getId().isBlank()) throw new RuntimeException("id is required");
        if (dto.getFirstName() == null || dto.getFirstName().isBlank()) throw new RuntimeException("firstName is required");
        if (dto.getLastName() == null || dto.getLastName().isBlank()) throw new RuntimeException("lastName is required");

        UserEntity owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Owner user not found"));

        if (studentRepository.existsByIdAndOwner_Email(dto.getId(), ownerEmail)) {
            throw new RuntimeException("Student id already exists");
        }

        String ext = normalizeExt(dto.getExternalId());
        if (ext != null && studentRepository.existsByExternalIdAndOwner_Email(ext, ownerEmail)) {
            throw new RuntimeException("externalId already exists");
        }

        StudentEntity s = new StudentEntity(dto.getId(), dto.getFirstName(), dto.getLastName(), ext, owner);
        s.setClasses(resolveClasses(ownerEmail, dto.getClassIds()));

        StudentEntity saved = studentRepository.save(s);
        return toDto(saved);
    }

    public StudentDto update(String ownerEmail, String id, StudentDto dto) {
        StudentEntity existing = studentRepository.findByIdAndOwner_Email(id, ownerEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (dto.getFirstName() != null) existing.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) existing.setLastName(dto.getLastName());

        String ext = normalizeExt(dto.getExternalId());
        if (ext != null) {
            // unic pe owner
            boolean usedByOther = studentRepository.findAllByOwner_EmailOrderByPkAsc(ownerEmail)
                    .stream()
                    .anyMatch(s -> s.getExternalId() != null && s.getExternalId().equals(ext) && !s.getId().equals(id));
            if (usedByOther) throw new RuntimeException("externalId already exists");
            existing.setExternalId(ext);
        } else if (dto.getExternalId() != null) {
            // daca trimite blank, il stergem
            existing.setExternalId(null);
        }

        if (dto.getClassIds() != null) {
            existing.setClasses(resolveClasses(ownerEmail, dto.getClassIds()));
        }

        StudentEntity saved = studentRepository.save(existing);
        return toDto(saved);
    }

    public void delete(String ownerEmail, String id) {
        StudentEntity existing = studentRepository.findByIdAndOwner_Email(id, ownerEmail)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        studentRepository.delete(existing);
    }

    private String normalizeExt(String externalId) {
        if (externalId == null) return null;
        String t = externalId.trim();
        return t.isBlank() ? null : t;
    }

    private Set<ClassEntity> resolveClasses(String ownerEmail, List<String> classIds) {
        Set<ClassEntity> set = new HashSet<>();
        if (classIds == null) return set;

        for (String cid : classIds) {
            if (cid == null || cid.isBlank()) continue;
            ClassEntity cls = classRepository.findByIdAndOwner_Email(cid.trim(), ownerEmail)
                    .orElseThrow(() -> new RuntimeException("Class not found: " + cid));
            set.add(cls);
        }
        return set;
    }

    private StudentDto toDto(StudentEntity e) {
        List<String> classIds = e.getClasses()
                .stream()
                .map(ClassEntity::getId)
                .sorted()
                .toList();
        return new StudentDto(e.getId(), e.getFirstName(), e.getLastName(), e.getExternalId(), classIds);
    }
}
