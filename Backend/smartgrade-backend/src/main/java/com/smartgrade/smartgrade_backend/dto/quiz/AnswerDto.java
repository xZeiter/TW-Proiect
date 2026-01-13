package com.smartgrade.smartgrade_backend.dto.quiz;

public class AnswerDto {
    private String text;
    private boolean correct;

    public AnswerDto() {}

    public AnswerDto(String text, boolean correct) {
        this.text = text;
        this.correct = correct;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }
}
