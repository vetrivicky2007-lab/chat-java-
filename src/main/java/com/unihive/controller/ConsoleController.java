package com.unihive.controller;

import com.unihive.dto.LoginRequest;
import com.unihive.dto.RegisterRequest;
import com.unihive.model.User;
import com.unihive.service.AuthService;
import com.unihive.service.AuthService.AuthResult;
import com.unihive.util.ConsoleColors;
import com.unihive.websocket.ChatClient;
import com.unihive.websocket.ChatServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Scanner;

/**
 * Main CLI controller for UniHive.
 *
 * <p>Implements {@link ApplicationRunner} so Spring Boot invokes
 * {@link #run(ApplicationArguments)} immediately after the application
 * context is fully started (and the {@link ChatServer} is already
 * listening). The method drives the entire interactive lifecycle:
 *
 * <pre>
 *  [Main Menu] → Register / Login / Exit
 *       ↓ on successful login
 *  [Chat Room] → read stdin → send over WebSocket → /exit → [Main Menu]
 * </pre>
 *
 * <p>This class contains <em>only</em> UI/flow logic. All business rules
 * live in {@link AuthService}. WebSocket messaging is handled by
 * {@link ChatClient}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsoleController implements ApplicationRunner {

    // ─────────────────────────────────────────────────────────────
    //  Dependencies
    // ─────────────────────────────────────────────────────────────

    private final AuthService  authService;
    private final ChatServer   chatServer;

    @Value("${unihive.websocket.port:8887}")
    private int wsPort;

    @Value("${unihive.websocket.host:localhost}")
    private String wsHost;

    // ─────────────────────────────────────────────────────────────
    //  Entry Point
    // ─────────────────────────────────────────────────────────────

    @Override
    public void run(ApplicationArguments args) {
        Scanner scanner = new Scanner(System.in);
        printBanner();

        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> handleRegistration(scanner);
                case "2" -> {
                    User user = handleLogin(scanner);
                    if (user != null) {
                        handleChatSession(scanner, user);
                    }
                }
                case "3" -> {
                    running = false;
                    printGoodbye();
                }
                default -> print(ConsoleColors.ERROR, "Invalid option. Please enter 1, 2, or 3.");
            }
        }

        // Graceful shutdown
        try {
            chatServer.stop(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Registration Flow
    // ─────────────────────────────────────────────────────────────

    private void handleRegistration(Scanner scanner) {
        printSectionHeader("Create Account");

        String username    = prompt(scanner, "Username");
        String email       = prompt(scanner, "Email");
        String password    = promptPassword(scanner, "Password");
        String confirmPass = promptPassword(scanner, "Confirm Password");

        RegisterRequest request = new RegisterRequest(username, email, password, confirmPass);
        AuthResult result = authService.register(request);

        if (result.success()) {
            print(ConsoleColors.SUCCESS, "✔  " + result.message());
        } else {
            print(ConsoleColors.ERROR, "✘  " + result.message());
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Login Flow
    // ─────────────────────────────────────────────────────────────

    /**
     * Handles the login flow.
     *
     * @return the authenticated {@link User} on success, {@code null} on failure
     */
    private User handleLogin(Scanner scanner) {
        printSectionHeader("Login");

        String username = prompt(scanner, "Username");
        String password = promptPassword(scanner, "Password");

        LoginRequest request = new LoginRequest(username, password);
        AuthResult result = authService.login(request);

        if (result.success()) {
            print(ConsoleColors.SUCCESS, "✔  " + result.message());
            return result.user();
        } else {
            print(ConsoleColors.ERROR, "✘  " + result.message());
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Chat Session
    // ─────────────────────────────────────────────────────────────

    /**
     * Opens a WebSocket chat session for the authenticated user.
     * Blocks until the user types {@code /exit}.
     *
     * @param scanner the shared stdin scanner
     * @param user    the logged-in user
     */
    private void handleChatSession(Scanner scanner, User user) {
        printChatHeader(user.getUsername());

        ChatClient client = null;
        try {
            URI serverUri = URI.create("ws://" + wsHost + ":" + wsPort);
            client = new ChatClient(serverUri, user.getUsername());
            client.connect();

            // Wait up to 10 seconds for the handshake to complete
            if (!client.waitForConnection(10)) {
                print(ConsoleColors.ERROR,
                      "✘  Could not connect to the chat server. Please try again.");
                return;
            }

            print(ConsoleColors.INFO,
                  "Connected! Type your message and press Enter. Type /exit to logout.");
            printDivider();

            // ── Blocking read loop ───────────────────────────────
            String inputLine;
            while (client.isActive()) {
                System.out.print(ConsoleColors.INFO + "You: " + ConsoleColors.RESET);

                if (!scanner.hasNextLine()) {
                    break; // EOF / pipe closed
                }
                inputLine = scanner.nextLine().trim();

                if (inputLine.equalsIgnoreCase("/exit")) {
                    break;
                }

                if (inputLine.isEmpty()) {
                    continue; // Ignore blank lines
                }

                client.sendChat(inputLine);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Chat session interrupted for user '{}'", user.getUsername());
        } catch (Exception e) {
            log.error("Chat session error for user '{}': {}", user.getUsername(), e.getMessage());
            print(ConsoleColors.ERROR, "✘  Connection error: " + e.getMessage());
        } finally {
            if (client != null && client.isActive()) {
                client.logout();
            }
            printDivider();
            print(ConsoleColors.SYSTEM_MSG, "You have left the chat. Returning to main menu...");
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  UI Helpers
    // ─────────────────────────────────────────────────────────────

    private void printBanner() {
        System.out.println();
        System.out.println(ConsoleColors.HEADER + "╔══════════════════════════════════════╗" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.HEADER + "║                                      ║" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.HEADER + "║   🐝  Welcome to UniHive  🐝          ║" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.HEADER + "║   Real-Time Console Chat  v1.0       ║" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.HEADER + "║                                      ║" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.HEADER + "╚══════════════════════════════════════╝" + ConsoleColors.RESET);
        System.out.println();
    }

    private void printMainMenu() {
        System.out.println();
        System.out.println(ConsoleColors.HEADER + "═══════════════════════════" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.BOLD_WHITE + "       Main  Menu          " + ConsoleColors.RESET);
        System.out.println(ConsoleColors.HEADER + "═══════════════════════════" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.WHITE + "  1.  Register" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.WHITE + "  2.  Login" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.WHITE + "  3.  Exit" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.HEADER + "═══════════════════════════" + ConsoleColors.RESET);
        System.out.print(ConsoleColors.INFO + "  Choose [1/2/3]: " + ConsoleColors.RESET);
    }

    private void printSectionHeader(String title) {
        System.out.println();
        System.out.println(ConsoleColors.HEADER + "─────────────────────────────" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.BOLD_CYAN + "  " + title + ConsoleColors.RESET);
        System.out.println(ConsoleColors.HEADER + "─────────────────────────────" + ConsoleColors.RESET);
    }

    private void printChatHeader(String username) {
        System.out.println();
        System.out.println(ConsoleColors.HEADER + "╔══════════════════════════════════════╗" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.HEADER + "║         🌐 Global Chat Room           ║" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.HEADER + "║  Logged in as: " + ConsoleColors.BOLD_GREEN
                + padRight(username, 22) + ConsoleColors.HEADER + "║" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.HEADER + "╚══════════════════════════════════════╝" + ConsoleColors.RESET);
    }

    private void printDivider() {
        System.out.println(ConsoleColors.HEADER + "─────────────────────────────────────────" + ConsoleColors.RESET);
    }

    private void printGoodbye() {
        System.out.println();
        System.out.println(ConsoleColors.SUCCESS + "  Thank you for using UniHive. Goodbye! 👋" + ConsoleColors.RESET);
        System.out.println();
    }

    private String prompt(Scanner scanner, String label) {
        System.out.print(ConsoleColors.INFO + "  " + label + ": " + ConsoleColors.RESET);
        return scanner.nextLine().trim();
    }

    /**
     * Reads a password from stdin.
     *
     * <p>Uses {@link System#console()} for masked input when available
     * (i.e., when running from a real terminal). Falls back to plain
     * {@code Scanner} read when running from an IDE or pipe where
     * {@code System.console()} returns {@code null}.
     */
    private String promptPassword(Scanner scanner, String label) {
    System.out.print(ConsoleColors.INFO + " " + label + ": " + ConsoleColors.RESET);
    return scanner.nextLine();
}

    private void print(String color, String message) {
        System.out.println(color + message + ConsoleColors.RESET);
    }

    private String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }
}
