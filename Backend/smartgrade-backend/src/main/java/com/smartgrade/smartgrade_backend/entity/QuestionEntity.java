package com.smartgrade.smartgrade_backend.entity;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "quiz_questions")
public class QuestionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pk;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_pk", nullable = false)
    private QuizEntity quiz;

    @Column(nullable = false, length = 2000)
    private String text;

    @Column(nullable = false)
    private int position;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<AnswerEntity> answers = new ArrayList<>();

    public QuestionEntity() {}

    public QuestionEntity(QuizEntity quiz, String text, int position) {
        this.quiz = quiz;
        this.text = text;
        this.position = position;
    }

    public Long getPk() { return pk; }

    public QuizEntity getQuiz() { return quiz; }
    public void setQuiz(QuizEntity quiz) { this.quiz = quiz; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public List<AnswerEntity> getAnswers() { return answers; }
    public void setAnswers(List<AnswerEntity> answers) { this.answers = answers; }
}
