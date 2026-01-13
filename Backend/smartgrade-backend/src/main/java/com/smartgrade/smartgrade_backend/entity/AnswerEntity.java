package com.smartgrade.smartgrade_backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "quiz_answers")
public class AnswerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pk;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "question_pk", nullable = false)
    private QuestionEntity question;

    @Column(nullable = false, length = 2000)
    private String text;

    @Column(nullable = false)
    private boolean correct;

    @Column(nullable = false)
    private int position;

    public AnswerEntity() {}

    public AnswerEntity(QuestionEntity question, String text, boolean correct, int position) {
        this.question = question;
        this.text = text;
        this.correct = correct;
        this.position = position;
    }

    public Long getPk() { return pk; }

    public QuestionEntity getQuestion() { return question; }
    public void setQuestion(QuestionEntity question) { this.question = question; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}
