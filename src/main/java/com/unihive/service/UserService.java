package com.unihive.service;

import com.unihive.model.User;
import com.unihive.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service providing general user-management operations.
 *
 * <p>Deliberately kept separate from {@link AuthService} to honour the
 * Single Responsibility Principle:
 * <ul>
 *   <li>{@link AuthService} owns <em>authentication</em> (register, login).
 *   <li>{@link UserService} owns <em>user data access and mutation</em>
 *       (lookups, profile updates, status changes).
 * </ul>
 *
 * <p>Version 1 exposes only what the chat room needs. All v2 methods
 * (update trust score, change role, community membership, etc.) should
 * be added here.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // ─────────────────────────────────────────────────────────────
    //  Lookups
    // ─────────────────────────────────────────────────────────────

    /**
     * Retrieves a user by username.
     *
     * @param username the username to search for
     * @return an {@link Optional} wrapping the found user, or empty
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Retrieves a user by their MongoDB ObjectId.
     *
     * @param id the user's document ID
     * @return an {@link Optional} wrapping the found user, or empty
     */
    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    /**
     * Checks whether a username is already registered.
     *
     * @param username the username to check
     * @return {@code true} if taken
     */
    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Checks whether an email is already registered.
     *
     * @param email the email to check
     * @return {@code true} if taken
     */
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    // ─────────────────────────────────────────────────────────────
    //  V2+ Extension Points
    // ─────────────────────────────────────────────────────────────

    /*
     * Update the user's online/offline status in the database.
     * Call this from ChatServer on open/close events when v2 requires
     * persistent presence tracking.
     *
     * public void updateOnlineStatus(String username, boolean online) {
     *     userRepository.findByUsername(username).ifPresent(user -> {
     *         user.setOnline(online);
     *         user.setLastSeenAt(Instant.now());
     *         userRepository.save(user);
     *         log.debug("User '{}' is now {}", username, online ? "ONLINE" : "OFFLINE");
     *     });
     * }
     */

    /*
     * Increment or decrement a user's Trust Score.
     *
     * public void adjustTrustScore(String username, int delta) {
     *     userRepository.findByUsername(username).ifPresent(user -> {
     *         int newScore = Math.max(0, user.getTrustScore() + delta);
     *         user.setTrustScore(newScore);
     *         userRepository.save(user);
     *         log.info("Trust score for '{}' adjusted by {} → {}", username, delta, newScore);
     *     });
     * }
     */

    /*
     * Assign a role to a user (e.g. "ADMIN", "MODERATOR").
     *
     * public void assignRole(String username, String role) {
     *     userRepository.findByUsername(username).ifPresent(user -> {
     *         user.setRole(role);
     *         userRepository.save(user);
     *         log.info("Role '{}' assigned to user '{}'", role, username);
     *     });
     * }
     */
}
