package com.example.demo3;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String URL      = "jdbc:mysql://localhost:3306/quiz_game";
    private static final String USER     = "root";
    private static final String PASSWORD = "yoyo1233";

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void initTables() {
        String[] sqls = {
                "CREATE TABLE IF NOT EXISTS users (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT," +
                        "username VARCHAR(100) UNIQUE NOT NULL," +
                        "password VARCHAR(255) NOT NULL)",

                "CREATE TABLE IF NOT EXISTS questions (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT," +
                        "question_text VARCHAR(500)," +
                        "option_a VARCHAR(100)," +
                        "option_b VARCHAR(100)," +
                        "option_c VARCHAR(100)," +
                        "correct_answer INT)",

                "CREATE TABLE IF NOT EXISTS game_results (" +
                        "id INT PRIMARY KEY AUTO_INCREMENT," +
                        "player_id INT," +
                        "user_name VARCHAR(100)," +
                        "score INT," +
                        "correct_answers INT," +
                        "wrong_answers INT)"
        };
        try (Connection conn = connect()) {
            for (String sql : sqls) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                }
            }
            System.out.println("[DB] Tables initialized successfully.");
        } catch (SQLException e) {
            System.err.println("[DB] FATAL – could not initialize tables: " + e.getMessage());
            System.err.println("[DB] Make sure the database 'quiz_game' exists in MySQL.");
        }
    }

    public static boolean authenticateOrRegister(String username, String password) {
        String checkSql = "SELECT password FROM users WHERE username = ?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    boolean ok = password.equals(rs.getString("password"));
                    System.out.println("[DB] Login attempt for '" + username + "': " + (ok ? "OK" : "WRONG PASSWORD"));
                    return ok;
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB] Auth check error: " + e.getMessage());
            return false;
        }

        String insertSql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.executeUpdate();
            System.out.println("[DB] Registered new user: " + username);
            return true;
        } catch (SQLException e) {
            System.err.println("[DB] Registration error for '" + username + "': " + e.getMessage());
            return false;
        }
    }

    public static List<Question> loadQuestions() {
        List<Question> questions = new ArrayList<>();
        String sql = "SELECT question_text, option_a, option_b, option_c, correct_answer " +
                "FROM questions ORDER BY RAND()";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String text = rs.getString("question_text");
                String[] options = {
                        rs.getString("option_a"),
                        rs.getString("option_b"),
                        rs.getString("option_c")
                };
                int correct = rs.getInt("correct_answer");
                questions.add(new Question(text, options, correct));
            }
            System.out.println("[DB] Loaded " + questions.size() + " questions.");
        } catch (SQLException e) {
            System.err.println("[DB] Failed to load questions: " + e.getMessage());
        }
        return questions;
    }

    public static void saveGameResult(int playerId, int score, int correct, int wrong, String userName) {
        String sql = "INSERT INTO game_results (player_id, user_name, score, correct_answers, wrong_answers) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playerId);
            ps.setString(2, userName);
            ps.setInt(3, score);
            ps.setInt(4, correct);
            ps.setInt(5, wrong);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] Failed to save result: " + e.getMessage());
        }
    }
}