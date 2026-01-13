package com.smartgrade.smartgrade_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgrade.smartgrade_backend.dto.results.SaveResultRequest;
import com.smartgrade.smartgrade_backend.dto.results.SaveResultResponse;
import com.smartgrade.smartgrade_backend.entity.*; 
import com.smartgrade.smartgrade_backend.repository.SheetRepository;
import com.smartgrade.smartgrade_backend.repository.SheetResultRepository;
import com.smartgrade.smartgrade_backend.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
public class ResultService {

    private final SheetRepository sheetRepository;
    private final SheetResultRepository sheetResultRepository;
    private final StudentRepository studentRepository;
    private final ObjectMapper objectMapper;

    public ResultService(
            SheetRepository sheetRepository,
            SheetResultRepository sheetResultRepository,
            StudentRepository studentRepository,
            ObjectMapper objectMapper
    ) {
        this.sheetRepository = sheetRepository;
        this.sheetResultRepository = sheetResultRepository;
        this.studentRepository = studentRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SaveResultResponse saveResult(String email, Long sheetId, SaveResultRequest body, boolean overwrite) {
        if (email == null || email.isBlank()) throw new RuntimeException("No user email");
        if (sheetId == null) throw new RuntimeException("sheetId is required");
        if (body == null) throw new RuntimeException("Body missing");
        if (body.getAnswers() == null) throw new RuntimeException("answers is required");

        SheetEntity sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new RuntimeException("Sheet not found"));

        if (sheet.getQuiz() == null || sheet.getQuiz().getOwner() == null) {
            throw new RuntimeException("Invalid sheet/quiz");
        }

        String ownerEmail = sheet.getQuiz().getOwner().getEmail();
        if (ownerEmail == null || !ownerEmail.equalsIgnoreCase(email)) {
            throw new RuntimeException("Not allowed");
        }

        Double calculatedScore = calculateScore(sheet.getQuiz(), body.getAnswers());
        System.out.println(">>> NOTA FINALA CALCULATA: " + calculatedScore);

        SheetResultEntity result = sheetResultRepository.findBySheetId(sheetId).orElse(null);

        if (result != null && !overwrite) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Result already exists for this sheet");
        }

        if (result == null) {
            result = new SheetResultEntity();
            result.setSheetId(sheet.getId());
            try {
                result.setQuizId(sheet.getQuiz().getId()); 
            } catch (Exception e) {}
        } else {
            result.setSheetId(sheet.getId());
            result.setQuizId(sheet.getQuiz().getId());
        }

        StudentEntity student = null;
        if (body.getStudentPk() != null) {
            student = studentRepository.findByPkAndOwner_Email(body.getStudentPk(), email)
                    .orElseThrow(() -> new RuntimeException("Student not found (or not yours)"));
        } else {
            String extId = body.getExtId();
            if (extId != null && !extId.isBlank()) {
                student = studentRepository.findByExternalIdAndOwner_Email(extId, email).orElse(null);
            }
        }

        String answersJson = writeJson(body.getAnswers());
        String answersConfJson = body.getAnswersConfidence() == null ? null : writeJson(body.getAnswersConfidence());

        Instant now = Instant.now();

        result.setStudent(student);
        result.setExtIdRaw(body.getExtId());
        result.setExtIdConfidence(body.getExtIdConfidence());
        result.setAnswersJson(answersJson);
        result.setAnswersConfidenceJson(answersConfJson);
        result.setScore(calculatedScore);

        result.setNameCropPath(body.getNameCropPath());
        if (body.getNameCropPath() != null && !body.getNameCropPath().isBlank()) {
            result.setNameCropKey(body.getNameCropPath());
        }

        result.setSavedAt(now);

        SheetResultEntity saved = sheetResultRepository.save(result);

        sheet.setStatus("SAVED");
        sheetRepository.save(sheet);

        String cropUrl = null;
        if (saved.getNameCropPath() != null) {
            String fullPath = saved.getNameCropPath();
            String normalizedPath = fullPath.replace("\\", "/");
            int index = normalizedPath.lastIndexOf("/crops/");
            if (index != -1) {
                String relativePath = normalizedPath.substring(index + 7); 
                cropUrl = "/crops/" + relativePath;
            } else {
                cropUrl = "/crops/" + new java.io.File(fullPath).getName();
            }
        }

        // --- MODIFICARE AICI: RETURNAM STRING ID ("SG-1") ---
        String detectedStudentId = (saved.getStudent() != null) ? saved.getStudent().getId() : null;

        return new SaveResultResponse(
            saved.getId(), 
            sheet.getId(), 
            sheet.getStatus(), 
            saved.getSavedAt(),
            cropUrl,
            detectedStudentId, // Acum e String
            saved.getScore()
        );
    }

    private String writeJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new RuntimeException("JSON serialize failed: " + e.getMessage());
        }
    }

    @Transactional
    public void updateResultStudent(String email, Long resultId, String studentId) {
        SheetResultEntity result = sheetResultRepository.findById(resultId)
                .orElseThrow(() -> new RuntimeException("Result not found with id: " + resultId));

        StudentEntity student = studentRepository.findByIdAndOwner_Email(studentId, email)
                .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));

        result.setStudent(student);
        sheetResultRepository.save(result);
    }

    @Transactional
    public void deleteResult(String email, Long resultId) {
        SheetResultEntity result = sheetResultRepository.findById(resultId)
                .orElseThrow(() -> new RuntimeException("Result not found"));
        sheetResultRepository.delete(result);
    }
    
    // --- MODIFICARE SI AICI PENTRU LISTA ---
    public List<SaveResultResponse> getResultsByQuiz(String email, String quizId) {
        List<SheetResultEntity> entities = sheetResultRepository.findAllByQuizId(quizId);
        List<SaveResultResponse> responses = new ArrayList<>();
        
        for (SheetResultEntity entity : entities) {
            String cropUrl = null;
            if (entity.getNameCropPath() != null) {
                String fullPath = entity.getNameCropPath().replace("\\", "/");
                int index = fullPath.lastIndexOf("/crops/");
                if (index != -1) {
                    cropUrl = "/crops/" + fullPath.substring(index + 7);
                } else {
                    cropUrl = "/crops/" + new java.io.File(fullPath).getName();
                }
            }

            // --- MODIFICARE AICI: RETURNAM STRING ID ---
            String studentId = entity.getStudent() != null ? entity.getStudent().getId() : null;
            
            responses.add(new SaveResultResponse(
                entity.getId(),
                entity.getSheetId(),
                "SAVED",
                entity.getSavedAt(),
                cropUrl,
                studentId, // String
                entity.getScore()
            ));
        }
        return responses;
    }

    // Algoritmul de calcul ramane la fel
    private double calculateScore(QuizEntity quiz, Map<Long, List<String>> scannedAnswers) {
        System.out.println("DEBUG PYTHON KEYS: " + scannedAnswers.keySet());

        List<QuestionEntity> questions = quiz.getQuestions();
        if (questions == null || questions.isEmpty()) return 0.0;

        questions.sort(Comparator.comparingInt(QuestionEntity::getPosition));
        List<Long> sortedScanKeys = new ArrayList<>(scannedAnswers.keySet());
        Collections.sort(sortedScanKeys);

        double maxGrade = 10.0;
        double pointsPerQuestion = maxGrade / questions.size();
        double totalScore = 0.0;

        for (int i = 0; i < questions.size(); i++) {
            QuestionEntity question = questions.get(i);
            Set<String> correctOptions = new HashSet<>();
            List<AnswerEntity> dbAnswers = question.getAnswers();
            if (dbAnswers != null) {
                for (int j = 0; j < dbAnswers.size(); j++) {
                    if (dbAnswers.get(j).isCorrect()) {
                        correctOptions.add(String.valueOf((char) ('A' + j)));
                    }
                }
            }

            List<String> studentSelection = new ArrayList<>();
            if (scannedAnswers.containsKey(question.getPk())) {
                studentSelection = scannedAnswers.get(question.getPk());
            } else if (scannedAnswers.containsKey((long) question.getPosition())) {
                studentSelection = scannedAnswers.get((long) question.getPosition());
            } else if (scannedAnswers.containsKey((long) question.getPosition() + 1)) {
                studentSelection = scannedAnswers.get((long) question.getPosition() + 1);
            } else if (i < sortedScanKeys.size()) {
                Long mappedKey = sortedScanKeys.get(i);
                studentSelection = scannedAnswers.get(mappedKey);
            }

            if (studentSelection == null) studentSelection = new ArrayList<>();

            boolean hasWrongAnswer = false;
            int correctHits = 0;
            for (String selected : studentSelection) {
                if (!correctOptions.contains(selected)) {
                    hasWrongAnswer = true;
                    break; 
                } else {
                    correctHits++;
                }
            }

            if (hasWrongAnswer) continue;

            if (!correctOptions.isEmpty()) {
                double valuePerOption = pointsPerQuestion / correctOptions.size();
                totalScore += (correctHits * valuePerOption);
            }
        }
        return Math.round(totalScore * 100.0) / 100.0;
    }
}