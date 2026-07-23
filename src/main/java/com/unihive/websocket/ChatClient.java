package com.unihive.websocket;

import com.unihive.dto.ChatMessage;
import com.unihive.util.ConsoleColors;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WebSocket client for a single UniHive chat session.
 *
 * <p>Each time a user successfully logs in, a new {@code ChatClient} is
 * created and connected to the local {@link ChatServer}. It:
 * <ul>
 *   <li>Sends a {@code JOIN} message to announce the user.
 *   <li>Prints all incoming messages (from the server's broadcast) to stdout.
 *   <li>Provides a {@link #sendChat(String)} method for outgoing messages.
 *   <li>Sends a {@code LEAVE} message and closes on {@link #logout()}.
 * </ul>
 *
 * <p>The client runs its receive loop on a Java-WebSocket background thread.
 * The calling thread (ConsoleController) handles the blocking stdin read loop
 * and calls {@link #sendChat(String)} / {@link #logout()} from there.
 */
@Slf4j
public class ChatClient extends WebSocketClient {

    // ─────────────────────────────────────────────────────────────
    //  State
    // ─────────────────────────────────────────────────────────────

    /** The authenticated username for this session. */
    private final String username;

    /** Signals when the WebSocket handshake has completed (or failed). */
    private final CountDownLatch connectLatch = new CountDownLatch(1);

    /** Signals when the WebSocket connection has fully closed. */
    private final CountDownLatch closeLatch   = new CountDownLatch(1);

    /** Whether this client is actively connected and in the chat room. */
    private final AtomicBoolean active = new AtomicBoolean(false);

    // ─────────────────────────────────────────────────────────────
    //  Constructor
    // ─────────────────────────────────────────────────────────────

    /**
     * Creates a new chat client for the given user.
     *
     * @param serverUri the WebSocket server URI (e.g. {@code ws://localhost:8887})
     * @param username  the authenticated username for this session
     */
    public ChatClient(URI serverUri, String username) {
        super(serverUri);
        this.username = username;
    }

    // ─────────────────────────────────────────────────────────────
    //  WebSocketClient Callbacks
    // ─────────────────────────────────────────────────────────────

    /**
     * Called when the connection to the server has been established.
     * Sends a JOIN message to register this user in the server's client map.
     */
    @Override
    public void onOpen(ServerHandshake handshake) {
        active.set(true);
        connectLatch.countDown(); // Unblocks waitForConnection()

        // Announce our presence to the server
        ChatMessage joinMsg = ChatMessage.builder()
                .type(ChatMessage.TYPE_JOIN)
                .sender(username)
                .content(username + " joined.")
                .build();
        send(joinMsg.serialise());
        log.debug("ChatClient connected and sent JOIN for user '{}'", username);
    }

    /**
     * Called when a broadcast message is received from the server.
     * Prints the message directly to stdout with a leading newline so it
     * doesn't overwrite the user's current input line.
     *
     * @param message the formatted message string from the server
     */
    @Override
    public void onMessage(String message) {
        // Print above the input prompt
        System.out.println("\r" + message);
        System.out.print(ConsoleColors.INFO + "You: " + ConsoleColors.RESET);
    }

    /**
     * Called when the connection is closed (by either side).
     */
    @Override
    public void onClose(int code, String reason, boolean remote) {
        active.set(false);
        closeLatch.countDown(); // Unblocks waitForClose()
        log.debug("ChatClient closed for user '{}' (code={}, reason='{}')", username, code, reason);
    }

    /**
     * Called when a WebSocket-level error occurs.
     */
    @Override
    public void onError(Exception ex) {
        log.error("ChatClient error for user '{}': {}", username, ex.getMessage());
        active.set(false);
        connectLatch.countDown(); // Prevent deadlock if error occurs before open
        closeLatch.countDown();
    }

    // ─────────────────────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────────────────────

    /**
     * Blocks the calling thread until the WebSocket connection is established
     * or the timeout expires.
     *
     * @param timeoutSeconds maximum seconds to wait
     * @return {@code true} if connected within the timeout
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public boolean waitForConnection(long timeoutSeconds) throws InterruptedException {
        return connectLatch.await(timeoutSeconds, TimeUnit.SECONDS) && active.get();
    }

    /**
     * Sends a chat message to the server, which will broadcast it to all clients.
     *
     * @param text the plain-text content to send
     */
    public void sendChat(String text) {
        if (!active.get()) {
            log.warn("Attempt to send chat while disconnected (user: '{}')", username);
            return;
        }
        ChatMessage chatMsg = ChatMessage.builder()
                .type(ChatMessage.TYPE_CHAT)
                .sender(username)
                .content(text)
                .build();
        send(chatMsg.serialise());
    }

    /**
     * Gracefully disconnects this client from the chat room.
     * Sends a LEAVE message first, then closes the WebSocket.
     */
    public void logout() {
        if (!active.get()) return;

        try {
            // Notify server of graceful departure
            ChatMessage leaveMsg = ChatMessage.builder()
                    .type(ChatMessage.TYPE_LEAVE)
                    .sender(username)
                    .content(username + " left.")
                    .build();
            send(leaveMsg.serialise());

            // Small delay to ensure the LEAVE frame is sent before closing
            Thread.sleep(200);
            close();
            closeLatch.await(5, TimeUnit.SECONDS);
            log.info("User '{}' logged out of chat.", username);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while logging out user '{}'", username);
        }
    }

    /**
     * @return {@code true} if this client is currently connected and active
     */
    public boolean isActive() {
        return active.get();
    }
}
