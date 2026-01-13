package com.smartgrade.smartgrade_backend.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "students",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_student_owner_studentid", columnNames = {"owner_user_id", "student_id"}),
                @UniqueConstraint(name = "uk_student_owner_externalid", columnNames = {"owner_user_id", "external_id"})
        }
)
public class StudentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pk;

    @Column(name = "student_id", nullable = false)
    private String id; // "SG-1"

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "external_id")
    private String externalId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private UserEntity owner;

    @ManyToMany
    @JoinTable(
            name = "student_classes",
            joinColumns = @JoinColumn(name = "student_pk"),
            inverseJoinColumns = @JoinColumn(name = "class_pk")
    )
    private Set<ClassEntity> classes = new HashSet<>();

    public StudentEntity() {}

    public StudentEntity(String id, String firstName, String lastName, String externalId, UserEntity owner) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.externalId = externalId;
        this.owner = owner;
    }

    public Long getPk() { return pk; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public UserEntity getOwner() { return owner; }
    public void setOwner(UserEntity owner) { this.owner = owner; }

    public Set<ClassEntity> getClasses() { return classes; }
    public void setClasses(Set<ClassEntity> classes) { this.classes = classes; }
}
