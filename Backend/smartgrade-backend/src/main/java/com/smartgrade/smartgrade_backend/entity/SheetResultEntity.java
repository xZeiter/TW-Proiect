package com.smartgrade.smartgrade_backend.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "sheet_results",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_result_sheet", columnNames = {"sheet_id"})
        }
)
public class SheetResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // EXISTING: keep as-is (used by current scan save)
    @Column(name = "sheet_id", nullable = false)
    private Long sheetId;

    // EXISTING: keep as-is (used by current scan save)
    @Column(name = "quiz_id", nullable = false, length = 80)
    private String quizId;

    // EXISTING: keep as-is
    @ManyToOne(optional = true)
    @JoinColumn(name = "student_pk")
    private StudentEntity student;

    // EXISTING: keep as-is (we will store extId here)
    @Column(name = "ext_id_raw", length = 50)
    private String extIdRaw;

    // EXISTING: keep as-is
    @Column(name = "ext_id_confidence")
    private Double extIdConfidence;

    // EXISTING: keep as-is
    @Lob
    @Column(name = "answers_json", nullable = false)
    private String answersJson;

    // EXISTING: keep as-is
    @Lob
    @Column(name = "scan_meta_json")
    private String scanMetaJson;

    // EXISTING: keep as-is (legacy pointer)
    @Column(name = "name_crop_key", length = 200)
    private String nameCropKey;

    // NEW: answers confidence JSON (optional)
    @Lob
    @Column(name = "answers_confidence_json")
    private String answersConfidenceJson;

    // NEW: name crop path (optional) - new field you asked
    @Column(name = "name_crop_path", length = 512)
    private String nameCropPath;
    
    // --- NEW FIELD: SCORE ---
    @Column(name = "score")
    private Double score;

    // EXISTING: keep created_at (used by old code / data)
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // NEW: saved_at (for new pipeline). keep nullable in DB for incremental safety.
    @Column(name = "saved_at")
    private Instant savedAt;

    // NEW (optional, view-only): relation to SheetEntity for easy owner-check without duplicating joins
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sheet_id", referencedColumnName = "id", insertable = false, updatable = false)
    private SheetEntity sheet;

    public SheetResultEntity() {}

    public SheetResultEntity(Long sheetId, String quizId, String answersJson) {
        this.sheetId = sheetId;
        this.quizId = quizId;
        this.answersJson = answersJson;
    }

    public Long getId() { return id; }

    public Long getSheetId() { return sheetId; }
    public void setSheetId(Long sheetId) { this.sheetId = sheetId; }

    public String getQuizId() { return quizId; }
    public void setQuizId(String quizId) { this.quizId = quizId; }

    public StudentEntity getStudent() { return student; }
    public void setStudent(StudentEntity student) { this.student = student; }

    public String getExtIdRaw() { return extIdRaw; }
    public void setExtIdRaw(String extIdRaw) { this.extIdRaw = extIdRaw; }

    public Double getExtIdConfidence() { return extIdConfidence; }
    public void setExtIdConfidence(Double extIdConfidence) { this.extIdConfidence = extIdConfidence; }

    public String getAnswersJson() { return answersJson; }
    public void setAnswersJson(String answersJson) { this.answersJson = answersJson; }

    public String getScanMetaJson() { return scanMetaJson; }
    public void setScanMetaJson(String scanMetaJson) { this.scanMetaJson = scanMetaJson; }

    public String getNameCropKey() { return nameCropKey; }
    public void setNameCropKey(String nameCropKey) { this.nameCropKey = nameCropKey; }

    public String getAnswersConfidenceJson() { return answersConfidenceJson; }
    public void setAnswersConfidenceJson(String answersConfidenceJson) { this.answersConfidenceJson = answersConfidenceJson; }

    public String getNameCropPath() { return nameCropPath; }
    public void setNameCropPath(String nameCropPath) { this.nameCropPath = nameCropPath; }
    
    // --- SCORE GETTER & SETTER ---
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getSavedAt() { return savedAt; }
    public void setSavedAt(Instant savedAt) { this.savedAt = savedAt; }

    public SheetEntity getSheet() { return sheet; }
    public void setSheet(SheetEntity sheet) { this.sheet = sheet; }
}