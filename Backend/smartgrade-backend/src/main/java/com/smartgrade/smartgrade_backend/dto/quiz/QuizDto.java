package com.smartgrade.smartgrade_backend.dto.quiz;

import java.util.List;

public class QuizDto {

    private String id;                 // "QZ-1"
    private String name;               // "Test Capitol 1"
    private String date;               // "2026-01-07"
    private List<String> classIds;     // ["CLS-1", "CLS-2"]
    private List<QuestionDto> questions;

    // --- NEW: Ext ID config ---
    private String extIdMode;           // DIGITS_ONLY / ALPHANUMERIC
    private Integer extIdLength;        // default 10

    // --- NEW: Layout info (read-only) ---
    private Integer layoutCurrentVersion;

    public QuizDto() {}

    public QuizDto(
            String id,
            String name,
            String date,
            List<String> classIds,
            List<QuestionDto> questions,
            String extIdMode,
            Integer extIdLength,
            Integer layoutCurrentVersion
    ) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.classIds = classIds;
        this.questions = questions;
        this.extIdMode = extIdMode;
        this.extIdLength = extIdLength;
        this.layoutCurrentVersion = layoutCurrentVersion;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public List<String> getClassIds() { return classIds; }
    public void setClassIds(List<String> classIds) { this.classIds = classIds; }

    public List<QuestionDto> getQuestions() { return questions; }
    public void setQuestions(List<QuestionDto> questions) { this.questions = questions; }

    public String getExtIdMode() { return extIdMode; }
    public void setExtIdMode(String extIdMode) { this.extIdMode = extIdMode; }

    public Integer getExtIdLength() { return extIdLength; }
    public void setExtIdLength(Integer extIdLength) { this.extIdLength = extIdLength; }

    public Integer getLayoutCurrentVersion() { return layoutCurrentVersion; }
    public void setLayoutCurrentVersion(Integer layoutCurrentVersion) {
        this.layoutCurrentVersion = layoutCurrentVersion;
    }
}
