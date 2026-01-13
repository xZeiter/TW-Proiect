package com.smartgrade.smartgrade_backend.service;

import com.smartgrade.smartgrade_backend.dto.scan.ScanUploadResponse;
import com.smartgrade.smartgrade_backend.entity.ExtIdMode;
import com.smartgrade.smartgrade_backend.entity.QuizEntity;
import com.smartgrade.smartgrade_backend.repository.QuizRepository;
import com.smartgrade.smartgrade_backend.service.scan.PythonScanClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class ScanOrchestratorService {

    private final PythonScanClient pythonScanClient;
    private final QuizRepository quizRepository;

    public ScanOrchestratorService(PythonScanClient pythonScanClient, QuizRepository quizRepository) {
        this.pythonScanClient = pythonScanClient;
        this.quizRepository = quizRepository;
    }

    public ScanUploadResponse scanAndValidate(String ownerEmail, MultipartFile file) {
        ScanUploadResponse resp = pythonScanClient.scan(file);
        if (resp == null) throw new RuntimeException("Empty scan response");
        if (resp.getQuizId() == null || resp.getQuizId().isBlank()) throw new RuntimeException("Scan missing quizId");
        if (resp.getSheetId() == null) throw new RuntimeException("Scan missing sheetId");

        QuizEntity quiz = quizRepository.findByIdAndOwner_Email(resp.getQuizId(), ownerEmail)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        
        if (resp.getExtId() != null) {
            resp.setExtId(resp.getExtId().trim().toUpperCase(Locale.ROOT));
        }

        boolean valid = validateExtId(resp.getExtId(), quiz);
        resp.setExtIdValid(valid);

        boolean lowConfidence = resp.getExtIdConfidence() == null || resp.getExtIdConfidence() < 0.80;
        resp.setNeedsManualStudentMatch(!valid || lowConfidence);

        return resp;
    }

    private boolean validateExtId(String extId, QuizEntity quiz) {
        if (extId == null || extId.isBlank()) return false;

        Integer len = quiz.getExtIdLength();
        if (len != null && extId.length() != len) return false;

        ExtIdMode mode = quiz.getExtIdMode() == null ? ExtIdMode.DIGITS_ONLY : quiz.getExtIdMode();
        if (mode == ExtIdMode.DIGITS_ONLY) return Pattern.matches("\\d+", extId);
        if (mode == ExtIdMode.ALPHANUMERIC) return Pattern.matches("[0-9A-Z]+", extId);

        return false;
    }
}
