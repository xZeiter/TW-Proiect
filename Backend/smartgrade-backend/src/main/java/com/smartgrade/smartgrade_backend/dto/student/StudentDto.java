package com.smartgrade.smartgrade_backend.dto.student;

import java.util.List;

public class StudentDto {
    private String id;
    private String firstName;
    private String lastName;
    private String externalId;
    private List<String> classIds;

    public StudentDto() {}

    public StudentDto(String id, String firstName, String lastName, String externalId, List<String> classIds) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.externalId = externalId;
        this.classIds = classIds;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public List<String> getClassIds() { return classIds; }
    public void setClassIds(List<String> classIds) { this.classIds = classIds; }
}
