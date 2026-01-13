package com.smartgrade.smartgrade_backend.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "classes")
public class ClassEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pk;

    @Column(nullable = false, unique = true)
    private String id; // "CLS-1"

    @Column(nullable = false)
    private String name;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private UserEntity owner;

    @ManyToMany(mappedBy = "classes")
    private Set<StudentEntity> students = new HashSet<>();
    public Set<StudentEntity> getStudents() { return students; }
    public void setStudents(Set<StudentEntity> students) { this.students = students; }

    public ClassEntity() {}

    public ClassEntity(String id, String name, UserEntity owner) {
        this.id = id;
        this.name = name;
        this.owner = owner;
    }

    public Long getPk() { return pk; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UserEntity getOwner() { return owner; }
    public void setOwner(UserEntity owner) { this.owner = owner; }
}
