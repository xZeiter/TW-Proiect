package com.smartgrade.smartgrade_backend.entity;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(
        name = "quizzes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_quiz_owner_quizid", columnNames = {"owner_user_id", "quiz_id"})
        }
)
public class QuizEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pk;

    @Column(name = "quiz_id", nullable = false)
    private String id; // "QZ-1"

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String date; // "YYYY-MM-DD"

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private UserEntity owner;

    @ManyToMany
    @JoinTable(
            name = "quiz_classes",
            joinColumns = @JoinColumn(name = "quiz_pk"),
            inverseJoinColumns = @JoinColumn(name = "class_pk")
    )
    private Set<ClassEntity> classes = new HashSet<>();

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<QuestionEntity> questions = new ArrayList<>();

    // --- NEW: Ext ID config per quiz ---
    @Enumerated(EnumType.STRING)
    @Column(name = "ext_id_mode", nullable = false, length = 20)
    private ExtIdMode extIdMode = ExtIdMode.DIGITS_ONLY;

    @Column(name = "ext_id_length", nullable = false)
    private Integer extIdLength = 10;

    // --- NEW: Layout version tracking (optional) ---
    @Column(name = "layout_current_version")
    private Integer layoutCurrentVersion;

    public QuizEntity() {}

    public QuizEntity(String id, String name, String date, UserEntity owner) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.owner = owner;
    }

    public Long getPk() { return pk; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public UserEntity getOwner() { return owner; }
    public void setOwner(UserEntity owner) { this.owner = owner; }

    public Set<ClassEntity> getClasses() { return classes; }
    public void setClasses(Set<ClassEntity> classes) { this.classes = classes; }

    public List<QuestionEntity> getQuestions() { return questions; }
    public void setQuestions(List<QuestionEntity> questions) { this.questions = questions; }

    public ExtIdMode getExtIdMode() { return extIdMode; }
    public void setExtIdMode(ExtIdMode extIdMode) { this.extIdMode = extIdMode; }

    public Integer getExtIdLength() { return extIdLength; }
    public void setExtIdLength(Integer extIdLength) { this.extIdLength = extIdLength; }

    public Integer getLayoutCurrentVersion() { return layoutCurrentVersion; }
    public void setLayoutCurrentVersion(Integer layoutCurrentVersion) { this.layoutCurrentVersion = layoutCurrentVersion; }
}
