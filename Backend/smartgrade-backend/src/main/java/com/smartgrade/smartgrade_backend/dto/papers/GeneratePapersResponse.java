package com.smartgrade.smartgrade_backend.dto.papers;

import java.util.List;

public class GeneratePapersResponse {

    private String quizId;
    private Integer layoutVersion;
    private List<Long> sheetIds;

    private String downloadUrl;

    public GeneratePapersResponse() {}

    public GeneratePapersResponse(String quizId, Integer layoutVersion, List<Long> sheetIds, String downloadUrl) {
        this.quizId = quizId;
        this.layoutVersion = layoutVersion;
        this.sheetIds = sheetIds;
        this.downloadUrl = downloadUrl;
    }

    public String getQuizId() { return quizId; }
    public void setQuizId(String quizId) { this.quizId = quizId; }

    public Integer getLayoutVersion() { return layoutVersion; }
    public void setLayoutVersion(Integer layoutVersion) { this.layoutVersion = layoutVersion; }

    public List<Long> getSheetIds() { return sheetIds; }
    public void setSheetIds(List<Long> sheetIds) { this.sheetIds = sheetIds; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
}
