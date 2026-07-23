package com.unihive.websocket;

import com.unihive.dto.ChatMessage;
import com.unihive.util.ConsoleColors;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket server for UniHive's global chat room.
 *
 * <p>Runs on a configurable port (default {@code 8887}) as a dedicated
 * thread inside the Spring Boot JVM. The server:
 * <ul>
 *   <li>Accepts multiple simultaneous client connections.
 *   <li>Maintains a {@link ConcurrentHashMap} mapping each WebSocket
 *       connection to its authenticated username.
 *   <li>Broadcasts every incoming message to <em>all</em> connected clients.
 *   <li>Announces join and leave events to the room.
 * </ul>
 *
 * <p><b>Thread safety:</b> {@link ConcurrentHashMap} is used for the
 * connection map. All Java-WebSocket callbacks run on the server's own
 * thread pool, so no additional synchronisation is needed.
 *
 * <p><b>Version 2 expansion:</b> The {@code rooms} map below is a hook
 * for multi-room support. In v1 every client is in {@code "global"}.
 */
@Slf4j
public class ChatServer extends WebSocketServer {

    // ─────────────────────────────────────────────────────────────
    //  State
    // ─────────────────────────────────────────────────────────────

    /**
     * Maps each active WebSocket connection to the authenticated username
     * that opened it. Thread-safe.
     */
    private final Map<WebSocket, String> connectedClients = new ConcurrentHashMap<>();

    /**
     * V2+ hook: maps room IDs to the set of WebSocket connections in that room.
     * In v1, only the {@code "global"} key is used (and is never explicitly set —
     * all clients are treated as global by default).
     *
     * <p>Uncomment and wire up routing logic in v2 when adding multiple rooms.
     */
    // private final Map<String, Set<WebSocket>> rooms = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────────
    //  Constructor
    // ─────────────────────────────────────────────────────────────

    /**
     * Creates the server bound to the given address.
     *
     * @param address the host/port to listen on (e.g. {@code new InetSocketAddress("localhost", 8887)})
     */
    public ChatServer(InetSocketAddress address) {
        super(address);
        setReuseAddr(true); // Allows quick restart without "address already in use" errors
    }

    // ─────────────────────────────────────────────────────────────
    //  WebSocketServer Callbacks
    // ─────────────────────────────────────────────────────────────

    /**
     * Called when a client successfully establishes a WebSocket connection.
     * The username is registered in the first message (a JOIN event).
     */
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        log.debug("New WebSocket connection from {}", conn.getRemoteSocketAddress());
        // Username registration happens in onMessage via the JOIN packet
    }

    /**
     * Called when a client sends a message.
     *
     * <p>Message format: pipe-delimited {@code TYPE|roomId|sender|content|timestamp}
     * (see {@link ChatMessage#serialise()}).
     *
     * <ul>
     *   <li>{@code JOIN}  — registers the username and notifies the room.
     *   <li>{@code LEAVE} — unregisters the username and notifies the room.
     *   <li>{@code CHAT}  — broadcasts the message to all connected clients.
     * </ul>
     */
    @Override
    public void onMessage(WebSocket conn, String rawMessage) {
        ChatMessage msg = ChatMessage.deserialise(rawMessage);
        if (msg == null) {
            log.warn("Received malformed message from {}: {}", conn.getRemoteSocketAddress(), rawMessage);
            return;
        }

        switch (msg.getType()) {
            case ChatMessage.TYPE_JOIN -> handleJoin(conn, msg);
            case ChatMessage.TYPE_LEAVE -> handleLeave(conn, msg);
            case ChatMessage.TYPE_CHAT -> handleChat(conn, msg);
            default -> log.warn("Unknown message type '{}' from '{}'", msg.getType(), msg.getSender());
        }
    }

    /**
     * Called when a client disconnects (normally or abnormally).
     * Cleans up the connection entry and notifies remaining clients.
     */
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String username = connectedClients.remove(conn);
        if (username != null) {
            log.info("User '{}' disconnected (code={}, reason='{}')", username, code, reason);
            String notification = ConsoleColors.SYSTEM_MSG
                    + "  ◀ " + username + " has left the chat."
                    + ConsoleColors.RESET;
            broadcast(notification);
        }
    }

    /**
     * Called when a WebSocket error occurs on a connection.
     */
    @Override
    public void onError(WebSocket conn, Exception ex) {
        String username = (conn != null) ? connectedClients.getOrDefault(conn, "unknown") : "unknown";
        log.error("WebSocket error for user '{}': {}", username, ex.getMessage());
    }

    /**
     * Called once the server has fully started and is ready to accept connections.
     */
    @Override
    public void onStart() {
        log.info("UniHive ChatServer started on port {}", getPort());
    }

    // ─────────────────────────────────────────────────────────────
    //  Private Handlers
    // ─────────────────────────────────────────────────────────────

    /**
     * Registers a joining user and broadcasts a join announcement.
     */
    private void handleJoin(WebSocket conn, ChatMessage msg) {
        connectedClients.put(conn, msg.getSender());
        log.info("User '{}' joined the global chat. Total online: {}", msg.getSender(), connectedClients.size());

        String announcement = ConsoleColors.SYSTEM_MSG
                + "  ▶ " + msg.getSender() + " has joined the chat. ("
                + connectedClients.size() + " online)"
                + ConsoleColors.RESET;
        broadcast(announcement);
    }

    /**
     * Removes a leaving user and broadcasts a leave announcement.
     */
    private void handleLeave(WebSocket conn, ChatMessage msg) {
        connectedClients.remove(conn);
        log.info("User '{}' left the global chat gracefully.", msg.getSender());

        String announcement = ConsoleColors.SYSTEM_MSG
                + "  ◀ " + msg.getSender() + " has left the chat."
                + ConsoleColors.RESET;
        broadcast(announcement);
    }

    /**
     * Formats and broadcasts a chat message to all connected clients.
     *
     * <p>Format: {@code username: message text}
     */
    private void handleChat(WebSocket conn, ChatMessage msg) {
        String sender = connectedClients.getOrDefault(conn, msg.getSender());
        log.debug("Chat from '{}': {}", sender, msg.getContent());

        String formatted = ConsoleColors.BOLD_CYAN + sender + ": "
                + ConsoleColors.RESET + msg.getContent();

        // Send to everyone except the sender
        for (WebSocket client : getConnections()) {
            if (!client.equals(conn)) {
                client.send(formatted);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Public Accessors
    // ─────────────────────────────────────────────────────────────

    /**
     * Returns an unmodifiable snapshot of currently connected usernames.
     *
     * @return set of online usernames
     */
    public Set<String> getOnlineUsers() {
        return Collections.unmodifiableSet(
                new java.util.HashSet<>(connectedClients.values()));
    }

    /**
     * Returns the number of currently connected clients.
     *
     * @return online count
     */
    public int getOnlineCount() {
        return connectedClients.size();
    }
}
