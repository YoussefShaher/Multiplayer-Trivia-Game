package com.example.demo3;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * GameServer - Main server application for multiplayer trivia game
 * 
 * Responsibilities:
 * - Accept client connections on port 5000
 * - Coordinate player authentication (CountDownLatch-based)
 * - Broadcast messages to all clients
 * - Trigger game start when ready
 * 
 * Thread model:
 * - Main thread: Server initialization, waits for 3 players
 * - Accept thread (daemon): Continuously accepts connections
 * - Client threads: One per connected client (created by ClientHandler)
 * - Game thread: Created by GameManager for game loop
 */
public class GameServer {
    private static final int PORT = 5000;
    private static final int PLAYER_COUNT = 3;

    // Thread-safe list of connected clients
    private static List<ClientHandler> players = Collections.synchronizedList(new ArrayList<>());
    
    // Synchronization mechanism: Blocks until PLAYER_COUNT clients authenticate
    private static CountDownLatch readyLatch = new CountDownLatch(PLAYER_COUNT);

    /**
     * Main entry point - Server startup
     * 
     * Algorithm:
     * 1. Initialize database tables
     * 2. Create server socket on port 5000
     * 3. Start daemon thread to accept connections
     * 4. Wait for 3 players to authenticate (via CountDownLatch)
     * 5. Initialize game with players
     * 6. Send START to all clients
     * 7. Run game manager
     * 
     * @param args Command line arguments (unused)
     * @throws Exception If server initialization fails
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Server started on port " + PORT);
        DatabaseManager.initTables();

        ServerSocket serverSocket = new ServerSocket(PORT);

        // Daemon thread to accept new connections
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
        readyLatch.await();  // Block until all 3 players authenticated

        System.out.println("All players ready. Starting game...");
        GameManager.initPlayers();

        // Send START to all clients
        for (ClientHandler p : players) {
            p.sendMessage("START");
        }

        GameManager.startGame();
    }

    /**
     * Registers authenticated player and notifies all clients
     * Called by ClientHandler after successful authentication
     * 
     * @param handler ClientHandler instance for the new player
     */
    public static synchronized void addAuthenticatedPlayer(ClientHandler handler) {
        handler.setPlayerId(players.size());
        players.add(handler);
        int count = players.size();
        System.out.println("Authenticated: " + handler.getPlayerName() + " (" + count + "/" + PLAYER_COUNT + ")");

        broadcastToAll("WAITING:" + count + "/" + PLAYER_COUNT);
    }

    /**
     * Decrements CountDownLatch
     * Called by each ClientHandler after authentication
     * When reaches 0, main thread unblocks and game starts
     */
    public static void triggerCountdown() {
        readyLatch.countDown();
    }

    /**
     * Broadcasts message to all connected clients
     * Used for questions, answers, chat, and game state updates
     * 
     * @param msg Pre-formatted message string
     */
    public static void broadcastToAll(String msg) {
        for (ClientHandler p : players) {
            p.sendMessage(msg);
        }
    }

    /**
     * Gets list of all authenticated players
     * @return Synchronized list of ClientHandler objects
     */
    public static List<ClientHandler> getPlayers() {
        return players;
    }
}
