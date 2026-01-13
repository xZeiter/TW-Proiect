package com.smartgrade.smartgrade_backend.dto.scan;

import java.util.List;

public class SaveResultRequest {

    private Long sheetId;
    private String quizId;

    private String studentId; // ID-ul tau extern sau pk? (il fac String ca sa decizi)
    private String extId;

    private List<ScanUploadResponse.AnswerReadDto> answers;

    private Double extIdConfidence;
    private String nameCropBase64; // optional (daca vrei sa salvezi)

    public SaveResultRequest() {}

    public Long getSheetId() { return sheetId; }
    public void setSheetId(Long sheetId) { this.sheetId = sheetId; }

    public String getQuizId() { return quizId; }
    public void setQuizId(String quizId) { this.quizId = quizId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getExtId() { return extId; }
    public void setExtId(String extId) { this.extId = extId; }

    public List<ScanUploadResponse.AnswerReadDto> getAnswers() { return answers; }
    public void setAnswers(List<ScanUploadResponse.AnswerReadDto> answers) { this.answers = answers; }

    public Double getExtIdConfidence() { return extIdConfidence; }
    public void setExtIdConfidence(Double extIdConfidence) { this.extIdConfidence = extIdConfidence; }

    public String getNameCropBase64() { return nameCropBase64; }
    public void setNameCropBase64(String nameCropBase64) { this.nameCropBase64 = nameCropBase64; }
}
