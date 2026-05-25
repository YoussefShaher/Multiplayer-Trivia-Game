package com.example.demo3;

/**
 * PlayerResult - Data model for final game results
 * 
 * Stores game outcome after match completion
 */
public class PlayerResult {
    public int id;
    public int score;
    public int correct;
    public int wrong;

    public PlayerResult(int id, int score, int correct, int wrong) {
        this.id = id;
        this.score = score;
        this.correct = correct;
        this.wrong = wrong;
    }
}
