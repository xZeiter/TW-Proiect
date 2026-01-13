package com.smartgrade.smartgrade_backend.dto.papers;

public class GeneratePapersRequest {

    private String classId;          // optional (ex: "CLS-1")
    private Boolean perStudentPdf;   // optional (default false)
    private Integer count;           // optional (default 1)

    public GeneratePapersRequest() {}

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public Boolean getPerStudentPdf() { return perStudentPdf; }
    public void setPerStudentPdf(Boolean perStudentPdf) { this.perStudentPdf = perStudentPdf; }

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
}
