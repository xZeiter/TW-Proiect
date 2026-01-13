package com.smartgrade.smartgrade_backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "sheets",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_sheet_quiz_sheetid", columnNames = {"quiz_pk", "id"})
        }
)
public class SheetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // sheetId (intra in QR)

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_pk", nullable = false)
    private QuizEntity quiz;

    @Column(name = "layout_version", nullable = false)
    private Integer layoutVersion;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt = Instant.now();

    @Column(nullable = false, length = 20)
    private String status; // GENERATED / SCANNED / SAVED

    public SheetEntity() {}

    public SheetEntity(QuizEntity quiz, Integer layoutVersion) {
        this.quiz = quiz;
        this.layoutVersion = layoutVersion;
        this.status = "GENERATED";
    }

    public Long getId() { return id; }

    public QuizEntity getQuiz() { return quiz; }
    public void setQuiz(QuizEntity quiz) { this.quiz = quiz; }

    public Integer getLayoutVersion() { return layoutVersion; }
    public void setLayoutVersion(Integer layoutVersion) { this.layoutVersion = layoutVersion; }

    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
