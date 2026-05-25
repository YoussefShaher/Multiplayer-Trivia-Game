package com.example.demo3;

public class Question {
    String question;
    String[] options;
    int correctAnswer;

    public Question(String question, String[] options, int correctAnswer) {
        this.question = question;
        this.options = options;
        this.correctAnswer = correctAnswer;
    }

    public String formatQuestion() {
        StringBuilder sb = new StringBuilder();
        sb.append("QUESTION:").append(question);
        for (int i = 0; i < options.length; i++) {
            sb.append("|").append(options[i]);
        }
        return sb.toString();
    }
}