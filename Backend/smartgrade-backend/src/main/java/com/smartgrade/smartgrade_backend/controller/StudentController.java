package com.smartgrade.smartgrade_backend.controller;

import com.smartgrade.smartgrade_backend.config.CurrentUser;
import com.smartgrade.smartgrade_backend.dto.student.StudentDto;
import com.smartgrade.smartgrade_backend.service.StudentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;
    private final CurrentUser currentUser;

    public StudentController(StudentService studentService, CurrentUser currentUser) {
        this.studentService = studentService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<StudentDto> all(HttpServletRequest request) {
        String email = currentUser.email(request);
        return studentService.findAll(email);
    }

    @PostMapping
    public StudentDto create(HttpServletRequest request, @RequestBody StudentDto s) {
        String email = currentUser.email(request);
        return studentService.create(email, s);
    }

    @PutMapping("/{id}")
    public StudentDto update(HttpServletRequest request, @PathVariable String id, @RequestBody StudentDto s) {
        String email = currentUser.email(request);
        return studentService.update(email, id, s);
    }

    @DeleteMapping("/{id}")
    public void delete(HttpServletRequest request, @PathVariable String id) {
        String email = currentUser.email(request);
        studentService.delete(email, id);
    }
}
