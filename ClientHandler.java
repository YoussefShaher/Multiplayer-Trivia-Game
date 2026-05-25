package com.example.demo3;

import java.net.*;
import java.io.*;

public class ClientHandler extends Thread {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private int playerId = -1;
    private String playerName;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            String message = in.readLine();
            if (message == null || !message.startsWith("LOGIN:")) {
                socket.close();
                return;
            }

            String[] parts = message.split(":", 3);
            if (parts.length < 3) {
                out.println("AUTH_FAIL");
                socket.close();
                return;
            }

            String username = parts[1];
            String password = parts[2];

            if (!DatabaseManager.authenticateOrRegister(username, password)) {
                out.println("AUTH_FAIL");
                socket.close();
                return;
            }

            this.playerName = username;
            GameServer.addAuthenticatedPlayer(this);

            out.println("ID:" + playerId);


            GameServer.triggerCountdown();
            String msg;
            while ((msg = in.readLine()) != null) {
                if (msg.startsWith("ANSWER:")) {
                    int answer = Integer.parseInt(msg.split(":")[1]);
                    GameManager.checkAnswer(playerId, answer);

                } else if (msg.startsWith("CHAT:")) {
                    String text = msg.substring(5);
                    GameServer.broadcastToAll("CHAT:" + playerName + ":" + text);
                }
            }
        } catch (IOException e) {
            System.out.println("Player disconnected: " + playerName);
        }
    }

    public synchronized void sendMessage(String msg) {
        out.println(msg);
    }

    public void setPlayerId(int id) {
        this.playerId = id;
    }

    public String getPlayerName() {
        return playerName;
    }
}