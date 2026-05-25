package com.example.demo3;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class GameServer {
    private static final int PORT = 5000;
    private static final int PLAYER_COUNT = 3;

    private static List<ClientHandler> players = Collections.synchronizedList(new ArrayList<>());
    private static CountDownLatch readyLatch = new CountDownLatch(PLAYER_COUNT);

    public static void main(String[] args) throws Exception {
        System.out.println("Server started on port " + PORT);
        DatabaseManager.initTables();

        ServerSocket serverSocket = new ServerSocket(PORT);

        Thread acceptThread = new Thread(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.println("New connection: " + socket.getInetAddress());
                    new ClientHandler(socket).start();
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) e.printStackTrace();
                }
            }
        });
        acceptThread.setDaemon(true);
        acceptThread.start();

        System.out.println("Waiting for " + PLAYER_COUNT + " players to authenticate...");
        readyLatch.await();

        System.out.println("All players ready. Starting game...");
        GameManager.initPlayers();

        for (ClientHandler p : players) {
            p.sendMessage("START");
        }

        GameManager.startGame();
    }

    public static synchronized void addAuthenticatedPlayer(ClientHandler handler) {
        handler.setPlayerId(players.size());
        players.add(handler);
        int count = players.size();
        System.out.println("Authenticated: " + handler.getPlayerName() + " (" + count + "/" + PLAYER_COUNT + ")");

        broadcastToAll("WAITING:" + count + "/" + PLAYER_COUNT);
    }

    public static void triggerCountdown() {
        readyLatch.countDown();
    }

    public static void broadcastToAll(String msg) {
        for (ClientHandler p : players) {
            p.sendMessage(msg);
        }
    }

    public static List<ClientHandler> getPlayers() {
        return players;
    }
}