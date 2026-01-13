package com.smartgrade.smartgrade_backend.controller;

import com.smartgrade.smartgrade_backend.config.CurrentUser;
import com.smartgrade.smartgrade_backend.dto.quiz.QuizDto;
import com.smartgrade.smartgrade_backend.service.QuizService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    private final QuizService quizService;
    private final CurrentUser currentUser;

    public QuizController(QuizService quizService, CurrentUser currentUser) {
        this.quizService = quizService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<QuizDto> all(HttpServletRequest request) {
        String email = currentUser.email(request);
        return quizService.findAll(email);
    }

    @PostMapping
    public QuizDto create(HttpServletRequest request, @RequestBody QuizDto dto) {
        String email = currentUser.email(request);
        return quizService.create(email, dto);
    }

    @PutMapping("/{id}")
    public QuizDto update(HttpServletRequest request, @PathVariable String id, @RequestBody QuizDto dto) {
        String email = currentUser.email(request);
        return quizService.update(email, id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(HttpServletRequest request, @PathVariable String id) {
        String email = currentUser.email(request);
        quizService.delete(email, id);
    }
}
