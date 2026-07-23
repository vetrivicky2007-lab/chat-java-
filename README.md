# UniHive

> **Version 1** — Console-Based Real-Time Chat Application  
> Multi-user global chat room powered by Java WebSocket and MongoDB Atlas.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Build Tool | Maven 3.8+ |
| Framework | Spring Boot 3.3 |
| Database | MongoDB Atlas (Cloud) |
| Data Access | Spring Data MongoDB |
| Real-Time | org.java-websocket 1.5.6 |
| Security | Spring Security Crypto (BCrypt) |
| Utilities | Lombok |

---

## Project Structure

```
unihive/
├── pom.xml
├── README.md
└── src/
    └── main/
        ├── java/com/unihive/
        │   ├── UniHiveApplication.java        ← Spring Boot entry point + ChatServer bean
        │   ├── config/
        │   │   ├── MongoConfig.java           ← MongoDB Atlas connection + auditing
        │   │   └── SecurityConfig.java        ← BCrypt bean (strength 12)
        │   ├── model/
        │   │   └── User.java                  ← MongoDB @Document
        │   ├── dto/
        │   │   ├── RegisterRequest.java       ← Registration input DTO
        │   │   ├── LoginRequest.java          ← Login input DTO
        │   │   └── ChatMessage.java           ← WebSocket message envelope
        │   ├── repository/
        │   │   └── UserRepository.java        ← Spring Data MongoDB repository
        │   ├── service/
        │   │   ├── AuthService.java           ← Register + Login logic
        │   │   └── UserService.java           ← User lookup helpers
        │   ├── websocket/
        │   │   ├── ChatServer.java            ← Java-WebSocket server (port 8887)
        │   │   └── ChatClient.java            ← Java-WebSocket client per session
        │   ├── controller/
        │   │   └── ConsoleController.java     ← CLI menu + chat loop
        │   └── util/
        │       ├── InputValidator.java        ← Validation rules
        │       └── ConsoleColors.java         ← ANSI terminal colours
        └── resources/
            └── application.properties        ← MongoDB URI + WebSocket port
```

---

## Prerequisites

- **Java 21** — `java -version` should show `21`
- **Maven 3.8+** — `mvn -version`
- **MongoDB Atlas account** — free tier at [cloud.mongodb.com](https://cloud.mongodb.com)

---

## Setup

### 1. Configure MongoDB Atlas

1. Sign in to [MongoDB Atlas](https://cloud.mongodb.com).
2. Create a free **M0** cluster (or use an existing one).
3. Add a **Database User** (username + password).
4. Whitelist your IP address in **Network Access** (or add `0.0.0.0/0` for development).
5. Click **Connect → Drivers** and copy your connection string. It looks like:
   ```
   mongodb+srv://myuser:mypassword@cluster0.abc12.mongodb.net/
   ```

### 2. Update `application.properties`

Open `src/main/resources/application.properties` and replace the placeholder URI:

```properties
# Before (placeholder):
spring.data.mongodb.uri=mongodb+srv://<username>:<password>@<cluster>.mongodb.net/unihive?retryWrites=true&w=majority

# After (your real URI):
spring.data.mongodb.uri=mongodb+srv://myuser:mypassword@cluster0.abc12.mongodb.net/unihive?retryWrites=true&w=majority
```

> **Note:** The database `unihive` and collection `users` will be created automatically on first run.

### 3. Build the Project

```bash
mvn clean package -DskipTests
```

---

## Running UniHive

### Option A — Maven Dev Mode (recommended during development)

```bash
mvn spring-boot:run
```

### Option B — Executable JAR

```bash
java -jar target/unihive-1.0.0.jar
```

> **Tip (Windows):** Run from **Windows Terminal** or **PowerShell** for ANSI colour support.  
> In older `cmd.exe`, colours may not render but the app still works correctly.

---

## Using the Application

### Main Menu

```
╔══════════════════════════════════════╗
║                                      ║
║   🐝  Welcome to UniHive  🐝          ║
║   Real-Time Console Chat  v1.0       ║
║                                      ║
╚══════════════════════════════════════╝

═══════════════════════════
       Main  Menu
═══════════════════════════
  1.  Register
  2.  Login
  3.  Exit
═══════════════════════════
  Choose [1/2/3]:
```

### Registering

Enter `1`, then provide:
- **Username** — 3–30 chars, letters/digits/underscore/hyphen, unique
- **Email** — valid email format, unique
- **Password** — minimum 8 characters
- **Confirm Password** — must match

### Logging In

Enter `2`, provide your username and password. On success you enter the global chat room.

### Chat Room

```
╔══════════════════════════════════════╗
║         🌐 Global Chat Room           ║
║  Logged in as: Alice                 ║
╚══════════════════════════════════════╝
Connected! Type your message and press Enter. Type /exit to logout.
You:
```

- Type any message and press **Enter** to send.
- All connected users see your message instantly.
- Type `/exit` to logout and return to the main menu.

### Multi-User Example

**Terminal 1 (Alice):**
```
You: Hello everyone!
```

**All terminals immediately see:**
```
Alice: Hello everyone!
```

**Terminal 2 (Bob):**
```
You: Hi Alice! Welcome!
```

**All terminals immediately see:**
```
Bob: Hi Alice! Welcome!
```

---

## Configuration Reference

```properties
# application.properties

# MongoDB Atlas — replace with your real connection string
spring.data.mongodb.uri=mongodb+srv://<username>:<password>@<cluster>.mongodb.net/unihive?retryWrites=true&w=majority

# WebSocket server port (change if 8887 is in use)
unihive.websocket.port=8887
unihive.websocket.host=localhost
```

---

## Architecture Notes

### Why Java-WebSocket + Spring Boot?

Spring Boot manages dependency injection, configuration, and MongoDB. The embedded Java-WebSocket server runs as a daemon thread inside the same JVM — no separate process needed.

### Message Format

WebSocket messages use a simple pipe-delimited format to avoid a JSON dependency in the client:

```
TYPE|roomId|sender|content|timestampEpochMillis
```

Example:
```
CHAT|global|Alice|Hello everyone!|1721650080000
```

### Security

- Passwords are **never** stored in plain text.
- BCrypt with strength 12 is used for hashing.
- Login errors are intentionally vague ("Invalid username or password") to prevent username enumeration.

---

## Future Expansion (v2+)

The architecture is designed for easy extension:

| Feature | Where to Add |
|---------|-------------|
| Multiple rooms | `ChatServer.rooms` map (hook already present) |
| Community CRUD | New `Community` model + `CommunityService` |
| Admin roles | Uncomment `role` field in `User.java` |
| Trust Score | Uncomment `trustScore` in `User.java` + `UserService.adjustTrustScore()` |
| Private messaging | `ChatMessage` already has `roomId`; add DM routing in `ChatServer` |
| Notifications | New `NotificationService` + `Notification` model |
| Persistence of messages | New `Message` model + `MessageRepository` |

---

## Troubleshooting

| Problem | Solution |
|---------|---------|
| `MongoTimeoutException` on startup | Check Atlas URI, whitelist your IP in Atlas Network Access |
| Port 8887 already in use | Change `unihive.websocket.port` in `application.properties` |
| No colours in terminal | Use Windows Terminal, PowerShell 7, or any Unix terminal |
| Password prompt shows typed characters | Expected in IDEs (no real console); use a terminal for masked input |
| `ClassNotFoundException` on Lombok | Run `mvn clean package` before running from IDE |

---

## License

MIT — Free to use, modify, and distribute.
