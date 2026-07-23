package com.unihive.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB document representing a UniHive user.
 *
 * <p>Version 1 stores core authentication and profile data.
 * Fields marked with "V2+" comments are intentionally left as
 * commented-out placeholders to guide future expansion without
 * requiring a schema migration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    // ─────────────────────────────────────────────────────────────
    //  Core Identity
    // ─────────────────────────────────────────────────────────────

    /** Auto-generated MongoDB ObjectId. */
    @Id
    private String id;

    /**
     * Unique, case-sensitive display name chosen during registration.
     * Used as the chat display name and login credential.
     */
    @Indexed(unique = true)
    @Field("username")
    private String username;

    /**
     * Unique email address. Used for account identification and
     * future password-reset flows.
     */
    @Indexed(unique = true)
    @Field("email")
    private String email;

    // ─────────────────────────────────────────────────────────────
    //  Security
    // ─────────────────────────────────────────────────────────────

    /**
     * BCrypt-hashed password. Plain-text passwords are NEVER stored.
     */
    @Field("passwordHash")
    private String passwordHash;

    // ─────────────────────────────────────────────────────────────
    //  Timestamps
    // ─────────────────────────────────────────────────────────────

    /** UTC timestamp of account creation. Set once on registration. */
    @CreatedDate
    @Field("createdAt")
    private Instant createdAt;

    // ─────────────────────────────────────────────────────────────
    //  V2+ Expansion Fields (uncomment when implementing Version 2)
    // ─────────────────────────────────────────────────────────────

    /*
     * Role assigned to the user. Default is "USER".
     * Future values: "ADMIN", "MODERATOR".
     *
     * @Field("role")
     * @Builder.Default
     * private String role = "USER";
     */

    /*
     * Trust Score accumulated through community interactions.
     * Higher score = more privileges in future versions.
     *
     * @Field("trustScore")
     * @Builder.Default
     * private int trustScore = 0;
     */

    /*
     * IDs of communities this user has joined.
     *
     * @Field("communityIds")
     * @Builder.Default
     * private List<String> communityIds = new ArrayList<>();
     */

    /*
     * Whether the user is currently online (connected via WebSocket).
     *
     * @Field("online")
     * @Builder.Default
     * private boolean online = false;
     */

    /*
     * UTC timestamp of the user's last activity.
     *
     * @Field("lastSeenAt")
     * private Instant lastSeenAt;
     */
}
