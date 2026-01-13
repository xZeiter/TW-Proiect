package com.smartgrade.smartgrade_backend.dto.results;

import java.time.Instant;

public class SaveResultResponse {

    private Long resultId;
    private Long sheetId;
    private String status;
    private Instant savedAt;
    private String nameCropUrl;
    
    // --- MODIFICARE AICI: Long -> String ---
    private String studentId; 
    
    private Double score;

    public SaveResultResponse() {}

    // Constructor actualizat
    public SaveResultResponse(Long resultId, Long sheetId, String status, Instant savedAt, String nameCropUrl, String studentId, Double score) {
        this.resultId = resultId;
        this.sheetId = sheetId;
        this.status = status;
        this.savedAt = savedAt;
        this.nameCropUrl = nameCropUrl;
        this.studentId = studentId;
        this.score = score;
    }

    public Long getResultId() { return resultId; }
    public void setResultId(Long resultId) { this.resultId = resultId; }

    public Long getSheetId() { return sheetId; }
    public void setSheetId(Long sheetId) { this.sheetId = sheetId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getSavedAt() { return savedAt; }
    public void setSavedAt(Instant savedAt) { this.savedAt = savedAt; }

    public String getNameCropUrl() { return nameCropUrl; }
    public void setNameCropUrl(String nameCropUrl) { this.nameCropUrl = nameCropUrl; }

    // --- GETTER/SETTER ACTUALIZATE ---
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
}