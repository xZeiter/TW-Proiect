package com.smartgrade.smartgrade_backend.controller;

import com.smartgrade.smartgrade_backend.config.CurrentUser;
import com.smartgrade.smartgrade_backend.dto.classroom.ClassDto;
import com.smartgrade.smartgrade_backend.service.ClassService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
public class ClassController {

    private final ClassService classService;
    private final CurrentUser currentUser;

    public ClassController(ClassService classService, CurrentUser currentUser) {
        this.classService = classService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<ClassDto> all(HttpServletRequest request) {
        String email = currentUser.email(request);
        return classService.getAll(email);
    }

    @PostMapping
    public ClassDto create(HttpServletRequest request, @RequestBody ClassDto dto) {
        String email = currentUser.email(request);
        return classService.create(email, dto);
    }

    @PutMapping("/{id}")
    public ClassDto update(HttpServletRequest request, @PathVariable String id, @RequestBody ClassDto dto) {
        String email = currentUser.email(request);
        return classService.update(email, id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(HttpServletRequest request, @PathVariable String id) {
        String email = currentUser.email(request);
        classService.delete(email, id);
    }
}
