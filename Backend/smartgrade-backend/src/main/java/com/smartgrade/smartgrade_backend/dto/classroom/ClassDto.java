package com.smartgrade.smartgrade_backend.dto.classroom;

public class ClassDto {
    private String id;
    private String name;

    public ClassDto() {}

    public ClassDto(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
