package com.example.demo3;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.net.Socket;

public class QuizGUIClient extends Application {

    private PrintWriter out;

    private Stage  primaryStage;
    private String playerName;
    private int    playerId = 0;
    private int    score    = 0;

    private Button joinBtn;
    private Label  errorLabel;


    private BorderPane mainLayout;

    private TextArea  chatArea;
    private TextField chatField;


    private boolean waitingScreenShown = false;
    private Label   waitingCountLabel;

    private Label       questionLabel;
    private VBox        optionsContainer;
    private ProgressBar timerBar;
    private Timeline    timerTimeline;
    private Label       statusLabel;
    private Label       scoreLabel;

    private static final String BG =
            "-fx-background-color: linear-gradient(to bottom right, #0f0c29, #302b63, #24243e);";

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("⚡ Quiz Battle");
        showLoginScreen();
    }

    private void showLoginScreen() {
        StackPane root = new StackPane();
        root.setStyle(BG);

        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40));
        card.setMaxWidth(400);
        card.setStyle(
                "-fx-background-color: rgba(12,10,38,0.88);" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-color: rgba(124,58,237,0.55);" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: 1.5;"
        );

        Label icon = new Label("⚡");
        icon.setStyle("-fx-font-size: 46px;");

        Label title = new Label("QUIZ BATTLE");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label subtitle = new Label("Multiplayer Trivia Challenge");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #7070a0;");

        Separator sep = new Separator();
        sep.setMaxWidth(260);

        TextField     usernameField = styledTextField("Enter your username");
        PasswordField passwordField = styledPasswordField("Enter your password");
        TextField     serverField   = styledTextField("Server (default: localhost)");
        serverField.setText("localhost");

        errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px;");
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(300);

        joinBtn = new Button("JOIN GAME  ▶");
        joinBtn.setMaxWidth(300);
        styleBtn(joinBtn, false);
        joinBtn.setOnMouseEntered(e -> styleBtn(joinBtn, true));
        joinBtn.setOnMouseExited(e  -> styleBtn(joinBtn, false));
        joinBtn.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();
            String server   = serverField.getText().trim().isEmpty()
                    ? "localhost" : serverField.getText().trim();
            if (username.isEmpty() || password.isEmpty()) {
                errorLabel.setText("⚠  Username and password are required.");
                return;
            }
            errorLabel.setText("");
            joinBtn.setDisable(true);
            joinBtn.setText("Connecting...");
            playerName = username;
            connectToServer(server, username, password);
        });

        card.getChildren().addAll(
                icon, title, subtitle, sep,
                labeledField("USERNAME", usernameField),
                labeledField("PASSWORD", passwordField),
                labeledField("SERVER",   serverField),
                errorLabel, joinBtn
        );

        root.getChildren().add(card);
        primaryStage.setScene(new Scene(root, 480, 580));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void connectToServer(String server, String username, String password) {
        new Thread(() -> {
            try {
                Socket socket = new Socket(server, 5000);
                out = new PrintWriter(socket.getOutputStream(), true);
                out.println("LOGIN:" + username + ":" + password);

                BufferedReader in =
                        new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String msg;
                while ((msg = in.readLine()) != null) {
                    final String m = msg;
                    Platform.runLater(() -> handleMessage(m));
                }
            } catch (IOException ex) {
                Platform.runLater(() -> {
                    errorLabel.setText("⚠  Cannot connect to " + server + ":5000");
                    joinBtn.setDisable(false);
                    joinBtn.setText("JOIN GAME  ▶");
                });
            }
        }).start();
    }

    private void handleMessage(String msg) {

        if (msg.startsWith("ID:")) {
            playerId = Integer.parseInt(msg.split(":")[1]);

        } else if (msg.equals("AUTH_FAIL")) {
            errorLabel.setText("⚠  Invalid username or password.");
            joinBtn.setDisable(false);
            joinBtn.setText("JOIN GAME  ▶");

        } else if (msg.startsWith("WAITING:")) {
            showWaitingScreen(msg.substring(8));       // e.g. "1/3"

        } else if (msg.equals("START")) {
            mainLayout.setCenter(buildGameCenter());   // swap center only; chat stays

        } else if (msg.startsWith("QUESTION:")) {
            displayQuestion(msg);

        } else if (msg.startsWith("WINNER:")) {
            String wid = msg.split(":")[1];
            stopTimer();
            optionsContainer.setDisable(true);
            if (String.valueOf(playerId).equals(wid)) {
                score += 10;
                scoreLabel.setText("Score: " + score);
                setStatus("✓  You got it!  +10 points", "#34d399");
            } else {
                setStatus("✗  Player " + wid + " answered first!", "#f87171");
            }

        } else if (msg.equals("NO_WINNER")) {
            stopTimer();
            optionsContainer.setDisable(true);
            setStatus("⏱  Time's up! No one answered.", "#fbbf24");

        } else if (msg.startsWith("STATS:")) {
            showFinalStats(msg);

        } else if (msg.startsWith("FINAL_WINNER:")) {

        } else if (msg.startsWith("CHAT:")) {
            String[] parts = msg.split(":", 3);
            if (parts.length >= 3) {
                appendChat(parts[1], parts[2]);
            }
        } else if (msg.equals("START")) {
        if (!waitingScreenShown) {          // safety guard if WAITING was missed
            buildMainLayout("?/" + "?");
        }
        mainLayout.setCenter(buildGameCenter());
        }
    }

    private void showWaitingScreen(String progress) {
        if (!waitingScreenShown) {
            waitingScreenShown = true;
            buildMainLayout(progress);
        } else if (waitingCountLabel != null) {
            waitingCountLabel.setText("Players connected:  " + progress);
        }
    }

    private void buildMainLayout(String progress) {
        mainLayout = new BorderPane();
        mainLayout.setStyle(BG);

        HBox topBar = new HBox(16);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(14, 24, 14, 24));
        topBar.setStyle("-fx-background-color: rgba(0,0,0,0.40);");

        Label gameTitle = new Label("⚡ QUIZ BATTLE");
        gameTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label nameTag = new Label("👤  " + playerName);
        nameTag.setStyle("-fx-font-size: 13px; -fx-text-fill: #9090c0;");

        scoreLabel = new Label("Score: 0");
        scoreLabel.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #fbbf24;" +
                        "-fx-background-color: rgba(251,191,36,0.13);" +
                        "-fx-padding: 5 13; -fx-background-radius: 20;"
        );

        topBar.getChildren().addAll(gameTitle, spacer, nameTag, scoreLabel);
        mainLayout.setTop(topBar);
        mainLayout.setRight(buildChatPanel());
        mainLayout.setCenter(buildWaitingCenter(progress));

        primaryStage.setScene(new Scene(mainLayout, 940, 530));
        primaryStage.setResizable(true);
    }

    private VBox buildChatPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(16));
        panel.setPrefWidth(248);
        panel.setStyle(
                "-fx-background-color: rgba(5,3,20,0.55);" +
                        "-fx-border-color: rgba(124,58,237,0.40);" +
                        "-fx-border-width: 0 0 0 1.5;"
        );

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label chatIcon  = new Label("💬");
        chatIcon.setStyle("-fx-font-size: 15px;");
        Label chatTitle = new Label("PLAYER CHAT");
        chatTitle.setStyle(
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #9d7dff;"
        );
        header.getChildren().addAll(chatIcon, chatTitle);

        Separator divider = new Separator();
        divider.setStyle("-fx-background-color: rgba(124,58,237,0.25);");

        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setPromptText("Messages will appear here…");
        chatArea.setStyle(
                "-fx-control-inner-background: rgba(10,8,30,0.80);" +
                        "-fx-text-fill: #d0d0f0;" +
                        "-fx-font-size: 12.5px;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: rgba(100,80,200,0.30);" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-width: 1;"
        );
        VBox.setVgrow(chatArea, Priority.ALWAYS);

        chatField = new TextField();
        chatField.setPromptText("Write a message…");
        chatField.setStyle(inputStyle());
        HBox.setHgrow(chatField, Priority.ALWAYS);

        Button sendBtn = new Button("Send");
        styleSendBtn(sendBtn, false);
        sendBtn.setOnMouseEntered(e -> styleSendBtn(sendBtn, true));
        sendBtn.setOnMouseExited(e  -> styleSendBtn(sendBtn, false));

        Runnable doSend = () -> {
            String text = chatField.getText().trim();
            if (!text.isEmpty() && out != null) {
                out.println("CHAT:" + text);
                chatField.clear();
                chatField.requestFocus();
            }
        };
        sendBtn.setOnAction(e -> doSend.run());
        chatField.setOnAction(e -> doSend.run());

        HBox inputRow = new HBox(8);
        inputRow.setAlignment(Pos.CENTER);
        inputRow.getChildren().addAll(chatField, sendBtn);

        panel.getChildren().addAll(header, divider, chatArea, inputRow);
        return panel;
    }

    private void appendChat(String sender, String text) {
        if (chatArea == null) return;
        String line = sender.equals(playerName)
                ? "You: " + text + "\n"
                : "👤 " + sender + ": " + text + "\n";
        chatArea.appendText(line);
    }

    private StackPane buildWaitingCenter(String progress) {
        StackPane sp = new StackPane();

        VBox card = new VBox(18);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(44));
        card.setMaxWidth(380);
        card.setStyle(
                "-fx-background-color: rgba(12,10,38,0.88);" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-color: rgba(124,58,237,0.55);" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: 1.5;"
        );

        Label icon = new Label("⏳");
        icon.setStyle("-fx-font-size: 46px;");

        Label heading = new Label("Waiting for Players");
        heading.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");

        waitingCountLabel = new Label("Players connected:  " + progress);
        waitingCountLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #a0a0c0;");

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(38, 38);
        spinner.setStyle("-fx-accent: #7c3aed;");

        Label hint = new Label("💬  You can chat with other players →");
        hint.setStyle("-fx-font-size: 11px; -fx-text-fill: #5050a0; -fx-font-style: italic;");

        card.getChildren().addAll(icon, heading, waitingCountLabel, spinner, hint);
        sp.getChildren().add(card);
        return sp;
    }

    private VBox buildGameCenter() {
        VBox center = new VBox(14);
        center.setAlignment(Pos.TOP_CENTER);
        center.setPadding(new Insets(22));

        timerBar = new ProgressBar(1.0);
        timerBar.setMaxWidth(Double.MAX_VALUE);
        timerBar.setPrefHeight(8);
        timerBar.setStyle("-fx-accent: #34d399;");

        statusLabel = new Label("Get ready!");
        statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7070a0;");

        VBox qCard = new VBox(20);
        qCard.setAlignment(Pos.CENTER);
        qCard.setPadding(new Insets(28, 36, 28, 36));
        qCard.setStyle(
                "-fx-background-color: rgba(12,10,38,0.75);" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: rgba(124,58,237,0.38);" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-width: 1.5;"
        );

        questionLabel = new Label("Waiting for first question…");
        questionLabel.setWrapText(true);
        questionLabel.setAlignment(Pos.CENTER);
        questionLabel.setMaxWidth(560);
        questionLabel.setStyle(
                "-fx-font-size: 19px; -fx-font-weight: bold;" +
                        "-fx-text-fill: white; -fx-text-alignment: center;"
        );

        optionsContainer = new VBox(10);
        optionsContainer.setAlignment(Pos.CENTER);

        qCard.getChildren().addAll(questionLabel, optionsContainer);
        center.getChildren().addAll(timerBar, statusLabel, qCard);
        return center;
    }

    private void displayQuestion(String msg) {
        String[] parts = msg.split("\\|");
        questionLabel.setText(parts[0].replace("QUESTION:", ""));
        setStatus("Choose your answer!", "#8080b0");
        optionsContainer.getChildren().clear();

        String[] colors  = {"#3b82f6", "#10b981", "#f59e0b"};
        String[] letters = {"A", "B", "C"};

        for (int i = 1; i < parts.length; i++) {
            final int    idx = i - 1;
            final String c   = colors[idx];

            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMaxWidth(520);

            Label badge = new Label(letters[idx]);
            badge.setMinWidth(36);
            badge.setAlignment(Pos.CENTER);
            badge.setStyle(
                    "-fx-background-color: " + c + ";" +
                            "-fx-text-fill: white; -fx-font-weight: bold;" +
                            "-fx-padding: 8 12; -fx-background-radius: 8;"
            );

            Button btn = new Button(parts[i]);
            btn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(btn, Priority.ALWAYS);

            String base = "-fx-background-color: rgba(255,255,255,0.06);" +
                    "-fx-text-fill: white; -fx-font-size: 14px;" +
                    "-fx-padding: 10 18; -fx-background-radius: 10;" +
                    "-fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 10;" +
                    "-fx-border-width: 1; -fx-cursor: hand; -fx-alignment: center-left;";
            String hover = "-fx-background-color: rgba(255,255,255,0.12);" +
                    "-fx-text-fill: white; -fx-font-size: 14px;" +
                    "-fx-padding: 10 18; -fx-background-radius: 10;" +
                    "-fx-border-color: " + c + "; -fx-border-radius: 10;" +
                    "-fx-border-width: 1.5; -fx-cursor: hand; -fx-alignment: center-left;";

            btn.setStyle(base);
            btn.setOnMouseEntered(e -> btn.setStyle(hover));
            btn.setOnMouseExited(e  -> btn.setStyle(base));
            btn.setOnAction(e -> {
                out.println("ANSWER:" + idx);
                optionsContainer.setDisable(true);
                setStatus("Answer submitted! Waiting for result…", "#818cf8");
            });

            row.getChildren().addAll(badge, btn);
            optionsContainer.getChildren().add(row);
        }

        optionsContainer.setDisable(false);
        startTimer();
    }

    private void startTimer() {
        if (timerTimeline != null) timerTimeline.stop();
        timerBar.setProgress(1.0);
        timerBar.setStyle("-fx-accent: #34d399;");
        timerTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,        new KeyValue(timerBar.progressProperty(), 1.0)),
                new KeyFrame(Duration.seconds(5),  e -> timerBar.setStyle("-fx-accent: #fbbf24;")),
                new KeyFrame(Duration.seconds(8),  e -> timerBar.setStyle("-fx-accent: #f87171;")),
                new KeyFrame(Duration.seconds(10), new KeyValue(timerBar.progressProperty(), 0.0))
        );
        timerTimeline.play();
    }

    private void stopTimer() {
        if (timerTimeline != null) timerTimeline.stop();
    }

    private void setStatus(String text, String color) {
        if (statusLabel != null) {
            statusLabel.setText(text);
            statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + color + ";");
        }
    }

    private void showFinalStats(String msg) {
        stopTimer();

        VBox content = new VBox(18);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(40));

        Label trophy = new Label("🏆");
        trophy.setStyle("-fx-font-size: 52px;");

        Label header = new Label("GAME OVER");
        header.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label sub = new Label("Final Standings");
        sub.setStyle("-fx-font-size: 14px; -fx-text-fill: #7070a0;");

        VBox statsCard = new VBox(0);
        statsCard.setStyle(
                "-fx-background-color: rgba(12,10,38,0.88);" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: rgba(124,58,237,0.45);" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-width: 1.5;"
        );

        String[] lines  = msg.replace("STATS:", "").split("\\|");
        String[] medals = {"🥇", "🥈", "🥉"};
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].isEmpty()) {
                HBox row = new HBox(14);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(14, 22, 14, 22));
                if (i > 0) row.setStyle(
                        "-fx-border-color: rgba(255,255,255,0.06); -fx-border-width: 1 0 0 0;"
                );
                Label medal = new Label(i < medals.length ? medals[i] : "   ");
                medal.setStyle("-fx-font-size: 18px;");
                Label data = new Label(lines[i]);
                data.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                row.getChildren().addAll(medal, data);
                statsCard.getChildren().add(row);
            }
        }

        Button exitBtn = new Button("EXIT GAME");
        exitBtn.setStyle(
                "-fx-background-color: #7c3aed; -fx-text-fill: white;" +
                        "-fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-padding: 12 36; -fx-background-radius: 10; -fx-cursor: hand;"
        );
        exitBtn.setOnAction(e -> System.exit(0));

        content.getChildren().addAll(trophy, header, sub, statsCard, exitBtn);

        mainLayout.setCenter(content);
    }

    private VBox labeledField(String label, Control field) {
        VBox box = new VBox(5);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #6060a0; -fx-font-size: 11px; -fx-font-weight: bold;");
        box.getChildren().addAll(lbl, field);
        return box;
    }

    private TextField styledTextField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setMaxWidth(300);
        f.setStyle(inputStyle());
        return f;
    }

    private PasswordField styledPasswordField(String prompt) {
        PasswordField f = new PasswordField();
        f.setPromptText(prompt);
        f.setMaxWidth(300);
        f.setStyle(inputStyle());
        return f;
    }

    private String inputStyle() {
        return "-fx-background-color: rgba(255,255,255,0.07);" +
                "-fx-text-fill: white;" +
                "-fx-prompt-text-fill: #404060;" +
                "-fx-padding: 10 14;" +
                "-fx-background-radius: 8;" +
                "-fx-border-color: rgba(100,80,200,0.4);" +
                "-fx-border-radius: 8;" +
                "-fx-border-width: 1;";
    }

    private void styleBtn(Button btn, boolean hover) {
        btn.setStyle(
                "-fx-background-color: " + (hover ? "#8b5cf6" : "#7c3aed") + ";" +
                        "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-padding: 12 0; -fx-background-radius: 10; -fx-cursor: hand;"
        );
    }

    private void styleSendBtn(Button btn, boolean hover) {
        btn.setStyle(
                "-fx-background-color: " + (hover ? "#8b5cf6" : "#7c3aed") + ";" +
                        "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
                        "-fx-padding: 9 14; -fx-background-radius: 8; -fx-cursor: hand;"
        );
    }

    public static void main(String[] args) {
        launch(args);
    }
}