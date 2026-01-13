package com.smartgrade.smartgrade_backend.dto.scan;

import java.util.List;

public class ScanUploadResponse {

    private String quizId;
    private Long sheetId;
    private Integer layoutVersion;

    private List<AnswerReadDto> answers;

    private String extId;
    private Double extIdConfidence;
    private Boolean extIdValid;

    private String nameCropBase64; // png/jpg base64
    private Boolean needsManualStudentMatch;

    public ScanUploadResponse() {}

    public String getQuizId() { return quizId; }
    public void setQuizId(String quizId) { this.quizId = quizId; }

    public Long getSheetId() { return sheetId; }
    public void setSheetId(Long sheetId) { this.sheetId = sheetId; }

    public Integer getLayoutVersion() { return layoutVersion; }
    public void setLayoutVersion(Integer layoutVersion) { this.layoutVersion = layoutVersion; }

    public List<AnswerReadDto> getAnswers() { return answers; }
    public void setAnswers(List<AnswerReadDto> answers) { this.answers = answers; }

    public String getExtId() { return extId; }
    public void setExtId(String extId) { this.extId = extId; }

    public Double getExtIdConfidence() { return extIdConfidence; }
    public void setExtIdConfidence(Double extIdConfidence) { this.extIdConfidence = extIdConfidence; }

    public Boolean getExtIdValid() { return extIdValid; }
    public void setExtIdValid(Boolean extIdValid) { this.extIdValid = extIdValid; }

    public String getNameCropBase64() { return nameCropBase64; }
    public void setNameCropBase64(String nameCropBase64) { this.nameCropBase64 = nameCropBase64; }

    public Boolean getNeedsManualStudentMatch() { return needsManualStudentMatch; }
    public void setNeedsManualStudentMatch(Boolean needsManualStudentMatch) {
        this.needsManualStudentMatch = needsManualStudentMatch;
    }

    // --- nested DTO ---
    public static class AnswerReadDto {
        private Integer questionIndex;
        private List<String> marked;     // ex ["A"] sau [] sau ["C","D"]
        private Double confidence;       // per question

        public AnswerReadDto() {}

        public AnswerReadDto(Integer questionIndex, List<String> marked, Double confidence) {
            this.questionIndex = questionIndex;
            this.marked = marked;
            this.confidence = confidence;
        }

        public Integer getQuestionIndex() { return questionIndex; }
        public void setQuestionIndex(Integer questionIndex) { this.questionIndex = questionIndex; }

        public List<String> getMarked() { return marked; }
        public void setMarked(List<String> marked) { this.marked = marked; }

        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }
    }
}
