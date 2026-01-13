package com.smartgrade.smartgrade_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgrade.smartgrade_backend.config.CurrentUser;
import com.smartgrade.smartgrade_backend.dto.scan.SaveResultRequest;
import com.smartgrade.smartgrade_backend.dto.scan.ScanUploadResponse;
import com.smartgrade.smartgrade_backend.entity.SheetEntity;
import com.smartgrade.smartgrade_backend.entity.SheetResultEntity;
import com.smartgrade.smartgrade_backend.entity.StudentEntity;
import com.smartgrade.smartgrade_backend.repository.SheetRepository;
import com.smartgrade.smartgrade_backend.repository.SheetResultRepository;
import com.smartgrade.smartgrade_backend.repository.StudentRepository;
import com.smartgrade.smartgrade_backend.service.ScanOrchestratorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/scan")
public class ScanController {

    private final CurrentUser currentUser;
    private final ScanOrchestratorService scanService;

    private final SheetRepository sheetRepository;
    private final SheetResultRepository sheetResultRepository;
    private final StudentRepository studentRepository;

    private final ObjectMapper objectMapper;

    public ScanController(
            CurrentUser currentUser,
            ScanOrchestratorService scanService,
            SheetRepository sheetRepository,
            SheetResultRepository sheetResultRepository,
            StudentRepository studentRepository,
            ObjectMapper objectMapper
    ) {
        this.currentUser = currentUser;
        this.scanService = scanService;
        this.sheetRepository = sheetRepository;
        this.sheetResultRepository = sheetResultRepository;
        this.studentRepository = studentRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/upload")
    public ScanUploadResponse upload(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file
    ) {
        String email = currentUser.email(request);
        return scanService.scanAndValidate(email, file);
    }

    @PostMapping("/save")
    public void save(
            HttpServletRequest request,
            @RequestBody SaveResultRequest body
    ) {
        String email = currentUser.email(request);

        if (body == null) throw new RuntimeException("Body missing");
        if (body.getSheetId() == null) throw new RuntimeException("sheetId is required");
        if (body.getQuizId() == null || body.getQuizId().isBlank()) throw new RuntimeException("quizId is required");

        // overwrite: daca exista deja, updatam acelasi rand (nu inseram altul)
SheetResultEntity result = sheetResultRepository.findBySheetId(body.getSheetId()).orElse(null);

if (result == null) {
    result = new SheetResultEntity(body.getSheetId(), body.getQuizId(), "[]"); // answersJson se pune mai jos
    result.setSheetId(body.getSheetId());
    result.setQuizId(body.getQuizId());
}


        // sheet must exist
        SheetEntity sheet = sheetRepository.findById(body.getSheetId())
                .orElseThrow(() -> new RuntimeException("Sheet not found"));
        if (sheet.getQuiz() == null || sheet.getQuiz().getId() == null) {
            throw new RuntimeException("Invalid sheet");
        }
        if (!sheet.getQuiz().getId().equals(body.getQuizId())) {
            throw new RuntimeException("Sheet does not belong to quiz " + body.getQuizId());
        }

        String answersJson = writeJson(body.getAnswers());
        result.setAnswersJson(answersJson);


        result.setExtIdRaw(body.getExtId());
        result.setExtIdConfidence(body.getExtIdConfidence());

        // student match:
        // - daca UI a ales studentId -> folosim ala
        // - altfel, daca avem extId -> incercam match automat
        StudentEntity chosen = null;

        if (body.getStudentId() != null && !body.getStudentId().isBlank()) {
            chosen = studentRepository.findByIdAndOwner_Email(body.getStudentId(), email)
                    .orElseThrow(() -> new RuntimeException("Student not found"));
        } else if (body.getExtId() != null && !body.getExtId().isBlank()) {
            chosen = studentRepository.findByExternalIdAndOwner_Email(body.getExtId().trim(), email).orElse(null);
        }

        result.setStudent(chosen);

        // scan meta (name crop etc)
        Map<String, Object> meta = new HashMap<>();
        meta.put("nameCropBase64", body.getNameCropBase64());
        meta.put("savedBy", email);
        meta.put("extIdConfidence", body.getExtIdConfidence());

        result.setScanMetaJson(writeJson(meta));

        sheetResultRepository.save(result);

        // optional status update
        sheet.setStatus("SAVED");
        sheetRepository.save(sheet);
    }

    private String writeJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new RuntimeException("JSON serialize failed: " + e.getMessage());
        }
    }
}
