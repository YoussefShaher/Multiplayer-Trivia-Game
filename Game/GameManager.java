package com.example.demo3;

import java.util.*;

public class GameManager {
    private static List<Question> questions = new ArrayList<>();
    private static List<Player> playScore = new ArrayList<>();
    private static int currentAnswer = -1;
    private static boolean answered = false;

    public static void initPlayers() {
        List<ClientHandler> handlers = GameServer.getPlayers();
        for (int i = 0; i < handlers.size(); i++) {
            playScore.add(new Player(i, handlers.get(i).getPlayerName()));
        }
    }

    public static void startGame() {
        List<Question> questions = DatabaseManager.loadQuestions();
        if (questions.isEmpty()) {
            System.err.println("No questions found in database!");
            return;
        }

        new Thread(() -> {
            try {
                for (Question q : questions) {
                    GameServer.broadcastToAll(q.formatQuestion());
                    currentAnswer = q.correctAnswer;
                    answered = false;
                    long startTime = System.currentTimeMillis();

                    while (!answered && (System.currentTimeMillis() - startTime < 10000)) {
                        Thread.sleep(100);
                    }

                    if (!answered) {
                        GameServer.broadcastToAll("NO_WINNER");
                    }
                    Thread.sleep(2000);
                }

                GameServer.broadcastToAll(getFinalStats());

                Player winner = getWinner();
                if (winner != null) {
                    GameServer.broadcastToAll("FINAL_WINNER:" + winner.id);
                }

                for (Player p : playScore) {
                    DatabaseManager.saveGameResult(p.id, p.score, p.correct, p.wrong, p.user_name);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static synchronized void checkAnswer(int playerId, int answer) {
        if (playerId < 0 || playerId >= playScore.size()) return;
        Player p = playScore.get(playerId);
        if (answer == currentAnswer && !answered) {
            answered = true;
            p.score += 10;
            p.correct++;
            GameServer.broadcastToAll("WINNER:" + playerId);
        } else if (answer != currentAnswer) {
            p.wrong++;
        }
    }

    private static String getFinalStats() {
        List<Player> sorted = new ArrayList<>(playScore);
        sorted.sort((a, b) -> b.score - a.score);

        StringBuilder sb = new StringBuilder("STATS:");
        for (Player p : sorted) {
            sb.append(p.name)
                    .append(" — Score: ").append(p.score)
                    .append("  ✓ ").append(p.correct)
                    .append("  ✗ ").append(p.wrong)
                    .append("|");
        }
        return sb.toString();
    }

    private static Player getWinner() {
        return playScore.stream().max(Comparator.comparingInt(p -> p.score)).orElse(null);
    }
}