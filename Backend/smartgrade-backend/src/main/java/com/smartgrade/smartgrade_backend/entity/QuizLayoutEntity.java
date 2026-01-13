package com.smartgrade.smartgrade_backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "quiz_layouts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_layout_quiz_version", columnNames = {"quiz_pk", "version"})
        }
)
public class QuizLayoutEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_pk", nullable = false)
    private QuizEntity quiz;

    @Column(nullable = false)
    private Integer version;

    @Lob
    @Column(name = "layout_json", nullable = false)
    private String layoutJson;

    @Column(name = "layout_hash", length = 64)
    private String layoutHash; // optional (SHA-256), pentru reuse

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public QuizLayoutEntity() {}

    public QuizLayoutEntity(QuizEntity quiz, Integer version, String layoutJson) {
        this.quiz = quiz;
        this.version = version;
        this.layoutJson = layoutJson;
    }

    public Long getId() { return id; }

    public QuizEntity getQuiz() { return quiz; }
    public void setQuiz(QuizEntity quiz) { this.quiz = quiz; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public String getLayoutJson() { return layoutJson; }
    public void setLayoutJson(String layoutJson) { this.layoutJson = layoutJson; }

    public String getLayoutHash() { return layoutHash; }
    public void setLayoutHash(String layoutHash) { this.layoutHash = layoutHash; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
