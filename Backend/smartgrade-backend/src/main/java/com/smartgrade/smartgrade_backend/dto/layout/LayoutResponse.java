package com.smartgrade.smartgrade_backend.dto.layout;

public class LayoutResponse {

    private String quizId;
    private Integer version;
    private String layoutJson;

    public LayoutResponse() {}

    public LayoutResponse(String quizId, Integer version, String layoutJson) {
        this.quizId = quizId;
        this.version = version;
        this.layoutJson = layoutJson;
    }

    public String getQuizId() { return quizId; }
    public void setQuizId(String quizId) { this.quizId = quizId; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public String getLayoutJson() { return layoutJson; }
    public void setLayoutJson(String layoutJson) { this.layoutJson = layoutJson; }
}
