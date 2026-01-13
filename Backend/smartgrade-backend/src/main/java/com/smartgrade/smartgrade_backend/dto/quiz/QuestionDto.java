package com.smartgrade.smartgrade_backend.dto.quiz;

import java.util.List;

public class QuestionDto {
    private String text;
    private List<AnswerDto> answers;

    public QuestionDto() {}

    public QuestionDto(String text, List<AnswerDto> answers) {
        this.text = text;
        this.answers = answers;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public List<AnswerDto> getAnswers() { return answers; }
    public void setAnswers(List<AnswerDto> answers) { this.answers = answers; }
}
