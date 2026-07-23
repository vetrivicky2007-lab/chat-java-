package com.unihive.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stateless utility class containing input validation rules for
 * all user-supplied fields in UniHive.
 *
 * <p>All methods are {@code static} — no instantiation required.
 * Centralising validation here keeps service classes clean and
 * makes it trivial to tighten or relax rules in one place.
 */
public final class InputValidator {

    // ─────────────────────────────────────────────────────────────
    //  Constants
    // ─────────────────────────────────────────────────────────────

    /** Minimum allowed length for a username. */
    public static final int USERNAME_MIN_LEN = 3;

    /** Maximum allowed length for a username. */
    public static final int USERNAME_MAX_LEN = 30;

    /** Minimum allowed length for a password. */
    public static final int PASSWORD_MIN_LEN = 8;

    /**
     * Username may only contain letters, digits, underscores, and hyphens.
     * Must start with a letter or digit.
     */
    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9_\\-]{" + (USERNAME_MIN_LEN - 1) + "," + (USERNAME_MAX_LEN - 1) + "}$");

    /** Email validation pattern requiring valid local part, @ symbol, valid domain label, and top-level domain. */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9\\-]+(\\.[a-zA-Z0-9\\-]+)*\\.[a-zA-Z]{2,}$");

    // ─────────────────────────────────────────────────────────────
    //  Constructor — prevent instantiation
    // ─────────────────────────────────────────────────────────────

    private InputValidator() {
        throw new UnsupportedOperationException("Utility class — do not instantiate.");
    }

    // ─────────────────────────────────────────────────────────────
    //  Validation Methods
    // ─────────────────────────────────────────────────────────────

    /**
     * Validates a username against length and character rules.
     *
     * @param username the username to validate
     * @return {@code null} if valid; a human-readable error message otherwise
     */
    public static String validateUsername(String username) {
        if (username == null || username.isBlank()) {
            return "Username cannot be empty.";
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return "Username must be " + USERNAME_MIN_LEN + "-" + USERNAME_MAX_LEN +
                   " characters, start with a letter or digit, and contain only " +
                   "letters, digits, underscores, or hyphens.";
        }
        return null; // valid
    }

    /**
     * Checks whether an email address format is valid using {@link Pattern} and {@link Matcher}.
     *
     * @param email the email to validate
     * @return {@code true} if format is valid; {@code false} otherwise
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        Matcher matcher = EMAIL_PATTERN.matcher(email.trim());
        return matcher.matches();
    }

    /**
     * Validates an email address format.
     *
     * @param email the email to validate
     * @return {@code null} if valid; a human-readable error message otherwise
     */
    public static String validateEmail(String email) {
        if (!isValidEmail(email)) {
            return "Invalid email format. Please enter a valid email address.";
        }
        return null; // valid
    }

    /**
     * Validates a password against minimum length and non-blank rules.
     *
     * @param password the plain-text password to validate
     * @return {@code null} if valid; a human-readable error message otherwise
     */
    public static String validatePassword(String password) {
        if (password == null || password.isBlank()) {
            return "Password cannot be empty.";
        }
        if (password.length() < PASSWORD_MIN_LEN) {
            return "Password must be at least " + PASSWORD_MIN_LEN + " characters long.";
        }
        return null; // valid
    }

    /**
     * Confirms that two password entries match.
     *
     * @param password        the original password
     * @param confirmPassword the confirmation entry
     * @return {@code null} if they match; a human-readable error message otherwise
     */
    public static String validatePasswordMatch(String password, String confirmPassword) {
        if (password == null || confirmPassword == null) {
            return "Password fields cannot be null.";
        }
        if (!password.equals(confirmPassword)) {
            return "Passwords do not match. Please try again.";
        }
        return null; // valid
    }

    /**
     * Checks whether a string is non-null and non-blank.
     *
     * @param value     the value to check
     * @param fieldName the human-readable field name for the error message
     * @return {@code null} if non-blank; a human-readable error message otherwise
     */
    public static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return fieldName + " cannot be empty.";
        }
        return null; // valid
    }
}
