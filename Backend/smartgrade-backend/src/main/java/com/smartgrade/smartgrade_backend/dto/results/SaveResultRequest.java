package com.smartgrade.smartgrade_backend.dto.results;

import java.util.List;
import java.util.Map;

public class SaveResultRequest {

    private String extId;
    private Double extIdConfidence;

    // questionPk -> ["A","C"]
    private Map<Long, List<String>> answers;

    // optional (any JSON-serializable object)
    private Object answersConfidence;

    // optional
    private Long studentPk;

    // optional
    private String nameCropPath;

    public SaveResultRequest() {}

    public String getExtId() { return extId; }
    public void setExtId(String extId) { this.extId = extId; }

    public Double getExtIdConfidence() { return extIdConfidence; }
    public void setExtIdConfidence(Double extIdConfidence) { this.extIdConfidence = extIdConfidence; }

    public Map<Long, List<String>> getAnswers() { return answers; }
    public void setAnswers(Map<Long, List<String>> answers) { this.answers = answers; }

    public Object getAnswersConfidence() { return answersConfidence; }
    public void setAnswersConfidence(Object answersConfidence) { this.answersConfidence = answersConfidence; }

    public Long getStudentPk() { return studentPk; }
    public void setStudentPk(Long studentPk) { this.studentPk = studentPk; }

    public String getNameCropPath() { return nameCropPath; }
    public void setNameCropPath(String nameCropPath) { this.nameCropPath = nameCropPath; }
}
