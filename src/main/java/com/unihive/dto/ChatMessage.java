package com.unihive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Envelope for messages exchanged over the WebSocket connection.
 *
 * <p>Serialised as a simple pipe-delimited string for transmission
 * (no JSON library needed in the console client):
 * <pre>TYPE|roomId|sender|content|timestamp</pre>
 *
 * <p>The {@code roomId} field defaults to {@code "global"} in Version 1
 * and is reserved for multi-room support in Version 2.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    // ─────────────────────────────────────────────────────────────
    //  Message Types
    // ─────────────────────────────────────────────────────────────

    /** Marker constants for the {@link #type} field. */
    public static final String TYPE_CHAT  = "CHAT";
    public static final String TYPE_JOIN  = "JOIN";
    public static final String TYPE_LEAVE = "LEAVE";

    // ─────────────────────────────────────────────────────────────
    //  Fields
    // ─────────────────────────────────────────────────────────────

    /**
     * One of {@link #TYPE_CHAT}, {@link #TYPE_JOIN}, {@link #TYPE_LEAVE}.
     */
    private String type;

    /**
     * Target room identifier. Defaults to {@code "global"} in v1.
     * Reserved for multi-room routing in v2.
     */
    @Builder.Default
    private String roomId = "global";

    /** Username of the message sender. */
    private String sender;

    /** The text body of the message. */
    private String content;

    /** UTC timestamp when the message was created on the client side. */
    @Builder.Default
    private Instant timestamp = Instant.now();

    // ─────────────────────────────────────────────────────────────
    //  Serialisation helpers
    // ─────────────────────────────────────────────────────────────

    /**
     * Serialises this message to a pipe-delimited string for WebSocket transport.
     *
     * @return pipe-delimited string: {@code TYPE|roomId|sender|content|timestamp}
     */
    public String serialise() {
        return type + "|" + roomId + "|" + sender + "|" + content + "|" + timestamp.toEpochMilli();
    }

    /**
     * Parses a pipe-delimited string produced by {@link #serialise()}.
     *
     * @param raw the raw WebSocket payload
     * @return a populated {@code ChatMessage}, or {@code null} if malformed
     */
    public static ChatMessage deserialise(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String[] parts = raw.split("\\|", 5);
        if (parts.length < 5) return null;
        try {
            return ChatMessage.builder()
                    .type(parts[0])
                    .roomId(parts[1])
                    .sender(parts[2])
                    .content(parts[3])
                    .timestamp(Instant.ofEpochMilli(Long.parseLong(parts[4])))
                    .build();
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
