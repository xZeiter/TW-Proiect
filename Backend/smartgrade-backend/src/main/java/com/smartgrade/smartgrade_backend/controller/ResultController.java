package com.smartgrade.smartgrade_backend.controller;

import com.smartgrade.smartgrade_backend.config.CurrentUser;
import com.smartgrade.smartgrade_backend.dto.results.SaveResultRequest;
import com.smartgrade.smartgrade_backend.dto.results.SaveResultResponse;
import com.smartgrade.smartgrade_backend.service.ResultService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sheets")
public class ResultController {

    private final CurrentUser currentUser;
    private final ResultService resultService;

    public ResultController(CurrentUser currentUser, ResultService resultService) {
        this.currentUser = currentUser;
        this.resultService = resultService;
    }

    @PostMapping("/{sheetId}/results")
    public SaveResultResponse save(
            HttpServletRequest request,
            @PathVariable Long sheetId,
            @RequestParam(defaultValue = "true") boolean overwrite,
            @RequestBody SaveResultRequest body
    ) {
        String email = currentUser.email(request);
        return resultService.saveResult(email, sheetId, body, overwrite);
    }

    @GetMapping("/quiz/{quizId}/results")
    public ResponseEntity<List<SaveResultResponse>> getQuizResults(
            HttpServletRequest request,
            @PathVariable String quizId
    ) {
        String email = currentUser.email(request);
        return ResponseEntity.ok(resultService.getResultsByQuiz(email, quizId));
    }

    // === MODIFICARE: studentId este acum STRING (ex: "SG-1") ===
    @PutMapping("/results/{resultId}/student/{studentId}")
    public ResponseEntity<?> updateStudent(
            HttpServletRequest request,
            @PathVariable Long resultId,
            @PathVariable String studentId 
    ) {
        String email = currentUser.email(request);
        try {
            resultService.updateResultStudent(email, resultId, studentId);
            return ResponseEntity.ok().body("{\"message\": \"Student updated successfully\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @DeleteMapping("/results/{resultId}")
    public ResponseEntity<?> deleteResult(
            HttpServletRequest request,
            @PathVariable Long resultId
    ) {
        String email = currentUser.email(request);
        try {
            resultService.deleteResult(email, resultId);
            return ResponseEntity.ok().body("{\"message\": \"Result deleted\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}