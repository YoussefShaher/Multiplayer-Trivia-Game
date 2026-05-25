package com.example.demo3;

public class Player {
    public String user_name;
    int id;
    String name;
    int score = 0;
    int correct = 0;
    int wrong = 0;

    public Player(int id, String name) {
        this.id = id;
        this.name = name;
        user_name = name;
    }
}
