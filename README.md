# ⚡ Multiplayer Trivia Game

A real-time, multi-threaded TCP trivia game featuring a polished JavaFX desktop interface and a MySQL backend that safely handles concurrent player connections and live scoreboard synchronization.

## 📋 Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Database Schema](#database-schema)
- [Communication Protocol](#communication-protocol)
- [Installation & Setup](#installation--setup)
- [How to Run](#how-to-run)
- [Game Features](#game-features)
- [Code Documentation](#code-documentation)
- [Learning Outcomes](#learning-outcomes)
- [Future Improvements](#future-improvements)

---

## ✨ Features

### Core Functionality
- **Real-time Multiplayer**: Support for exactly 3 concurrent players
- **Live Scoring**: Instant score updates and leaderboard display
- **Secure Authentication**: Login/Registration with database persistence
- **Chat System**: Real-time player communication during gameplay
- **Beautiful UI**: Modern JavaFX interface with smooth animations

### Technical Features
- **Multi-threaded Server**: Concurrent connection handling using Java threads
- **Thread-Safe Operations**: Synchronized lists and methods for data consistency
- **Countdown Timer**: 10-second question timeout with visual progress bar
- **Database Integration**: MySQL backend for persistent data storage
- **Protocol-Based Communication**: Structured message passing over TCP sockets

---

## 🏗️ Architecture

### System Design
```
┌─────────────────────────────────────────────────────┐
│                     GameServer                       │
│  - Listens on port 5000                             │
│  - Waits for 3 players using CountDownLatch         │
│  - Broadcasts messages to all clients               │
└────────────┬────────────────────────────────────────┘
             │
    ┌────────┼────────┬──────────┐
    │        │        │          │
    ▼        ▼        ▼          ▼
┌────────┐ ┌────────┐ ┌────────┐ ┌──────────────┐
│Client1 │ │Client2 │ │Client3 │ │ GameManager  │
│Handler │ │Handler │ │Handler │ │ - Game logic │
│Thread  │ │Thread  │ │Thread  │ │ - Scoring    │
└────────┘ └────────┘ └────────┘ └──────────────┘
    │        │        │
    └────────┼────────┘
             │
    ┌────────▼─────────────┐
    │  DatabaseManager     │
    │  - Authentication    │
    │  - Questions         │
    │  - Results           │
    └──────────────────────┘
             │
    ┌────────▼─────────────┐
    │   MySQL Database     │
    │  - users table       │
    │  - questions table   │
    │  - game_results      │
    └──────────────────────┘
```

### Thread Model

**Main Thread (GameServer)**
- Initializes database and server socket
- Accepts client connections
- Waits for player authentication via CountDownLatch
- Triggers game start

**Accept Thread (Daemon)**
- Runs continuously to accept new connections
- Creates a new ClientHandler thread for each client

**Client Handler Threads (Per Client)**
- Handle login/registration
- Listen for player answers and chat messages
- Relay messages to appropriate handlers

**Game Thread**
- Runs game loop in separate thread to avoid blocking
- Loads and broadcasts questions
- Waits for answers with 10-second timeout
- Calculates and broadcasts statistics

---

## 📁 Project Structure

```
src/com/example/demo3/
├── GameServer.java          # Main server application
├── ClientHandler.java        # Individual client connection handler
├── GameManager.java          # Game logic and scoring
├── DatabaseManager.java      # Database operations
├── Player.java               # Player data model
├── Question.java             # Question data model
├── PlayerResult.java         # Game result data model
└── QuizGUIClient.java        # JavaFX client application
```

### File Descriptions

| File | Purpose | Key Responsibilities |
|------|---------|----------------------|
| **GameServer.java** | Main server | Listen for connections, coordinate game start, broadcast messages |
| **ClientHandler.java** | Client connection | Authenticate players, route messages, send/receive data |
| **GameManager.java** | Game logic | Questions flow, answer validation, scoring, statistics |
| **DatabaseManager.java** | Data persistence | User authentication, question loading, result saving |
| **QuizGUIClient.java** | Client UI | JavaFX interface, question display, timer, chat |
| **Player.java** | Data model | Track player score, correct/wrong answers |
| **Question.java** | Data model | Store question, options, correct answer |
| **PlayerResult.java** | Data model | Store final game results |

---

## 🗄️ Database Schema

### MySQL Database: `quiz_game`

#### Table: `users`
Stores player credentials for authentication/registration
```sql
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL
);
```

#### Table: `questions`
Stores trivia questions with multiple choice options
```sql
CREATE TABLE questions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    question_text VARCHAR(500),
    option_a VARCHAR(100),
    option_b VARCHAR(100),
    option_c VARCHAR(100),
    correct_answer INT              -- 0 for A, 1 for B, 2 for C
);
```

#### Table: `game_results`
Stores player performance after each game
```sql
CREATE TABLE game_results (
    id INT PRIMARY KEY AUTO_INCREMENT,
    player_id INT,
    user_name VARCHAR(100),
    score INT,
    correct_answers INT,
    wrong_answers INT
);
```

---

## 🔌 Communication Protocol

### Message Format
All messages use pipe (`|`) and colon (`:`) delimiters for easy parsing.

### Server → Client Messages

| Message | Format | Example | Description |
|---------|--------|---------|-------------|
| **ID** | `ID:playerId` | `ID:0` | Sends player their unique ID |
| **AUTH_FAIL** | `AUTH_FAIL` | `AUTH_FAIL` | Authentication/registration failed |
| **WAITING** | `WAITING:count/total` | `WAITING:2/3` | Player count update |
| **START** | `START` | `START` | Game is starting |
| **QUESTION** | `QUESTION:text\|optA\|optB\|optC` | `QUESTION:What is 2+2?\|3\|4\|5` | New question |
| **WINNER** | `WINNER:playerId` | `WINNER:1` | Player answered correctly |
| **NO_WINNER** | `NO_WINNER` | `NO_WINNER` | Time expired, no one answered |
| **CHAT** | `CHAT:username:message` | `CHAT:John:Good luck!` | Chat message from player |
| **STATS** | `STATS:player1 — Score: 20 ✓ 2 ✗ 0\|...` | Stats display | Final game statistics |
| **FINAL_WINNER** | `FINAL_WINNER:playerId` | `FINAL_WINNER:0` | Overall game winner |

### Client → Server Messages

| Message | Format | Example | Description |
|---------|--------|---------|-------------|
| **LOGIN** | `LOGIN:username:password` | `LOGIN:john:secret123` | Authentication request |
| **ANSWER** | `ANSWER:optionIndex` | `ANSWER:1` | Player submits answer (0, 1, or 2) |
| **CHAT** | `CHAT:message` | `CHAT:Great question!` | Send chat message |

---

## 🚀 Installation & Setup

### Prerequisites
- **Java**: JDK 11 or higher
- **JavaFX**: JavaFX SDK 11+ (for GUI)
- **MySQL**: MySQL Server 5.7 or higher
- **JDBC Driver**: mysql-connector-java (included or add to classpath)

### Step 1: Create MySQL Database

```sql
CREATE DATABASE quiz_game;
USE quiz_game;
```

### Step 2: Add Sample Questions

```sql
INSERT INTO questions (question_text, option_a, option_b, option_c, correct_answer) 
VALUES 
('What is the capital of France?', 'London', 'Paris', 'Berlin', 1),
('What is 2 + 2?', '3', '4', '5', 1),
('Which planet is closest to the Sun?', 'Venus', 'Mercury', 'Earth', 1),
('What is the largest ocean?', 'Atlantic', 'Arctic', 'Pacific', 2),
('Who wrote Romeo and Juliet?', 'Shakespeare', 'Marlowe', 'Bacon', 0),
('What is the chemical symbol for Gold?', 'Go', 'Gd', 'Au', 2),
('How many continents are there?', '6', '7', '8', 1),
('What year did the Titanic sink?', '1912', '1920', '1905', 0);
```

### Step 3: Update Database Credentials

Edit `DatabaseManager.java` and update these fields:
```java
private static final String URL      = "jdbc:mysql://localhost:3306/quiz_game";
private static final String USER     = "root";
private static final String PASSWORD = "your_password";
```

### Step 4: Compile Project

```bash
javac src/com/example/demo3/*.java
```

Or using Maven/Gradle with proper JavaFX configuration.

---

## ▶️ How to Run

### Terminal 1: Start Server
```bash
java -cp bin:mysql-connector-java.jar com.example.demo3.GameServer
```

Expected output:
```
Server started on port 5000
[DB] Tables initialized successfully.
Waiting for 3 players to authenticate...
```

### Terminal 2-4: Start 3 Clients
```bash
java --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls \
     -cp bin:mysql-connector-java.jar com.example.demo3.QuizGUIClient
```

Or if using IDE (IntelliJ, Eclipse, etc.):
- Import project
- Add JavaFX and MySQL JDBC to build path
- Run as Java Application

### Game Flow
1. **Player Login**: Enter username, password, server address
2. **Waiting Room**: Chat while waiting for other players
3. **Game Start**: 10 seconds per question, multiple choice
4. **Live Scoring**: See scores update in real-time
5. **Results**: Final leaderboard after all questions

---

## 🎮 Game Features

### Gameplay Mechanics
- **10-Second Timer**: Visual progress bar with color transitions (green → yellow → red)
- **Instant Feedback**: See who answered first and correct/incorrect status
- **Live Chat**: Communicate with other players anytime
- **Score Calculation**: +10 points per correct answer
- **Leaderboard**: Final standings with medals (🥇🥈🥉)

### UI Components
- **Login Screen**: Username, password, server selection
- **Top Bar**: Player name, live score display
- **Question Card**: Centered question with color-coded options
- **Chat Panel**: Real-time messaging with other players
- **Results Screen**: Medals and final statistics

---

## 📚 Code Documentation

### Key Classes

#### GameServer
- **Purpose**: Main server orchestration
- **Key Methods**:
  - `main()`: Server startup and game initialization
  - `addAuthenticatedPlayer()`: Register new player
  - `broadcastToAll()`: Send message to all clients
  - `triggerCountdown()`: Decrement ready latch

#### ClientHandler
- **Purpose**: Per-client communication
- **Key Methods**:
  - `run()`: Main thread loop for message handling
  - `sendMessage()`: Thread-safe message sending
  - `setPlayerId()`: Assign player ID

#### GameManager
- **Purpose**: Game logic and flow control
- **Key Methods**:
  - `startGame()`: Main game loop
  - `checkAnswer()`: Validate player answer
  - `getFinalStats()`: Format leaderboard
  - `getWinner()`: Determine overall winner

#### DatabaseManager
- **Purpose**: Data persistence
- **Key Methods**:
  - `initTables()`: Create database schema
  - `authenticateOrRegister()`: User authentication
  - `loadQuestions()`: Fetch randomized questions
  - `saveGameResult()`: Persist game results

#### QuizGUIClient
- **Purpose**: JavaFX client interface
- **Key Methods**:
  - `showLoginScreen()`: Display login UI
  - `handleMessage()`: Process server messages
  - `displayQuestion()`: Show question to player
  - `startTimer()`: Begin 10-second countdown

---

## 🎓 Learning Outcomes

This project demonstrates:

### Java Fundamentals
- ✅ Multi-threading with Thread class and Thread safety
- ✅ Collections (ArrayList, synchronized lists)
- ✅ Exception handling (IOException, SQLException)
- ✅ Try-with-resources for resource management

### Network Programming
- ✅ Socket programming (TCP)
- ✅ Client-server architecture
- ✅ Message-based communication protocol
- ✅ Concurrent connection handling

### Database
- ✅ JDBC and prepared statements
- ✅ MySQL database design
- ✅ Connection pooling concepts

### GUI Development
- ✅ JavaFX fundamentals
- ✅ Layouts and styling with CSS
- ✅ Event handling
- ✅ Animation and Timeline

### Concurrency
- ✅ Thread coordination with CountDownLatch
- ✅ Synchronized methods
- ✅ Concurrent data structures
- ✅ Platform.runLater() for thread-safe UI updates

---

## ⚠️ Known Issues & Limitations

1. **Passwords**: Currently stored in plain text (use bcrypt in production)
2. **Session Management**: No logout mechanism, relies on disconnect
3. **Error Handling**: Limited error recovery in network failures
4. **Player Count**: Fixed to exactly 3 players (hardcoded)
5. **Question Limit**: Game must have questions in database to start

---

## 🔮 Future Improvements

### High Priority
- [ ] Hash passwords using bcrypt or Argon2
- [ ] Add player disconnection handling
- [ ] Implement configurable player count
- [ ] Add database connection pooling
- [ ] Implement reconnection logic

### Medium Priority
- [ ] Add game difficulty levels
- [ ] Power-ups and special abilities
- [ ] Ranked matchmaking
- [ ] Player statistics/history
- [ ] Customizable themes

### Low Priority
- [ ] Mobile client (Android/iOS)
- [ ] Web interface (Swing → JSP/Spring)
- [ ] Voice chat integration
- [ ] Spectator mode
- [ ] Tournament support

---

## 📖 Usage Examples

### Adding More Questions
```sql
INSERT INTO questions VALUES 
(NULL, 'Your question here?', 'Option A', 'Option B', 'Option C', 1);
```

### Viewing Player History
```sql
SELECT user_name, score, correct_answers, wrong_answers 
FROM game_results 
ORDER BY score DESC;
```

### Modifying Question Format
Edit `Question.formatQuestion()` to change the message format sent to clients.

---

## 🤝 Contributing

To extend this project:

1. **Add Features**: Modify GameManager.java for new game mechanics
2. **Improve UI**: Enhance QuizGUIClient.java styling
3. **Add Database Features**: Extend DatabaseManager.java
4. **Handle Errors**: Add try-catch blocks for robustness

---

## 📝 License

This project is open source and available under the MIT License.

---

## 👨‍💻 Author

**Youssef Shaher**  
Created as a demonstration of multi-threaded network programming and real-time multiplayer game development in Java.

---

## 📞 Support & Questions

For issues or questions:
1. Check the code comments (extensive documentation provided)
2. Review the communication protocol section
3. Verify MySQL database is running
4. Check that all 3 clients connect before game starts

---

**Happy Gaming! ⚡🎮**
