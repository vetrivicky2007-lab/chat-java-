package com.unihive.service;

import com.unihive.dto.LoginRequest;
import com.unihive.dto.RegisterRequest;
import com.unihive.model.User;
import com.unihive.repository.UserRepository;
import com.unihive.util.InputValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Authentication service for UniHive.
 *
 * <p>Handles all registration and login business logic:
 * <ul>
 *   <li>Input validation (format, length, patterns)
 *   <li>Uniqueness checks (username, email)
 *   <li>BCrypt password hashing on registration
 *   <li>BCrypt password verification on login
 * </ul>
 *
 * <p>This class never returns or logs plain-text passwords.
 *
 * <p>All public methods return an {@link AuthResult} record that carries
 * either a success payload or a human-readable error message, keeping
 * the calling {@link com.unihive.controller.ConsoleController} free of
 * business logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository    userRepository;
    private final PasswordEncoder   passwordEncoder;

    // ─────────────────────────────────────────────────────────────
    //  Result Type
    // ─────────────────────────────────────────────────────────────

    /**
     * Sealed result envelope returned by every auth operation.
     *
     * @param success whether the operation succeeded
     * @param user    the authenticated/registered user; {@code null} on failure
     * @param message a human-readable success or error message
     */
    public record AuthResult(boolean success, User user, String message) {

        /** Convenience factory for a successful result. */
        static AuthResult ok(User user, String message) {
            return new AuthResult(true, user, message);
        }

        /** Convenience factory for a failed result. */
        static AuthResult fail(String message) {
            return new AuthResult(false, null, message);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Registration
    // ─────────────────────────────────────────────────────────────

    /**
     * Registers a new user.
     *
     * <p>Validation order:
     * <ol>
     *   <li>Username format/length
     *   <li>Email format
     *   <li>Password length
     *   <li>Password confirmation match
     *   <li>Username uniqueness (DB check)
     *   <li>Email uniqueness (DB check)
     * </ol>
     *
     * @param request the registration DTO from the console
     * @return an {@link AuthResult} with the new {@link User} on success,
     *         or an error message on failure
     */
    public AuthResult register(RegisterRequest request) {
        // ── 1. Input format validation ──────────────────────────
        String usernameError = InputValidator.validateUsername(request.getUsername());
        if (usernameError != null) {
            return AuthResult.fail(usernameError);
        }

        String emailError = InputValidator.validateEmail(request.getEmail());
        if (emailError != null) {
            return AuthResult.fail(emailError);
        }

        String passwordError = InputValidator.validatePassword(request.getPassword());
        if (passwordError != null) {
            return AuthResult.fail(passwordError);
        }

        String matchError = InputValidator.validatePasswordMatch(
                request.getPassword(), request.getConfirmPassword());
        if (matchError != null) {
            return AuthResult.fail(matchError);
        }

        // ── 2. Uniqueness checks ────────────────────────────────
        if (userRepository.existsByUsername(request.getUsername())) {
            return AuthResult.fail("Username '" + request.getUsername() + "' is already taken. Please choose another.");
        }

        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            return AuthResult.fail("An account with email '" + request.getEmail() + "' already exists.");
        }

        // ── 3. Persist ──────────────────────────────────────────
        try {
            User newUser = User.builder()
                    .username(request.getUsername().trim())
                    .email(request.getEmail().trim().toLowerCase())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .createdAt(Instant.now())
                    .build();

            User saved = userRepository.save(newUser);
            log.info("New user registered: '{}'", saved.getUsername());
            return AuthResult.ok(saved, "Account created successfully! Welcome to UniHive, " + saved.getUsername() + "!");

        } catch (Exception e) {
            log.error("Registration failed for username '{}': {}", request.getUsername(), e.getMessage());
            return AuthResult.fail("Registration failed due to a database error. Please try again.");
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Login
    // ─────────────────────────────────────────────────────────────

    /**
     * Authenticates a user with username and password.
     *
     * @param request the login DTO from the console
     * @return an {@link AuthResult} with the authenticated {@link User} on success,
     *         or a generic error message on failure (intentionally vague for security)
     */
    public AuthResult login(LoginRequest request) {
        // ── 1. Basic input presence check ───────────────────────
        String usernameError = InputValidator.requireNonBlank(request.getUsername(), "Username");
        if (usernameError != null) {
            return AuthResult.fail(usernameError);
        }

        String passwordError = InputValidator.requireNonBlank(request.getPassword(), "Password");
        if (passwordError != null) {
            return AuthResult.fail(passwordError);
        }

        // ── 2. Lookup user ──────────────────────────────────────
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername().trim());

        // Intentionally generic error — do not reveal whether the username exists
        if (userOpt.isEmpty()) {
            log.warn("Login attempt with unknown username: '{}'", request.getUsername());
            return AuthResult.fail("Invalid username or password.");
        }

        User user = userOpt.get();

        // ── 3. Verify BCrypt hash ───────────────────────────────
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Failed login attempt for user: '{}'", user.getUsername());
            return AuthResult.fail("Invalid username or password.");
        }

        log.info("User '{}' logged in successfully.", user.getUsername());
        return AuthResult.ok(user, "Login successful! Welcome back, " + user.getUsername() + "!");
    }
}
