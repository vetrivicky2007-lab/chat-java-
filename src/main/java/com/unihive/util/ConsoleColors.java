package com.unihive.util;

/**
 * ANSI escape-code constants for coloured console output.
 *
 * <p>Works out-of-the-box on Linux, macOS, and Windows Terminal (10+).
 * On older Windows Command Prompt, colours may not render; the text will
 * still be readable — just without colour.
 *
 * <p>Usage example:
 * <pre>{@code
 *   System.out.println(ConsoleColors.GREEN + "Success!" + ConsoleColors.RESET);
 * }</pre>
 */
public final class ConsoleColors {

    // ─────────────────────────────────────────────────────────────
    //  Reset
    // ─────────────────────────────────────────────────────────────

    public static final String RESET   = "\033[0m";

    // ─────────────────────────────────────────────────────────────
    //  Regular Colours
    // ─────────────────────────────────────────────────────────────

    public static final String BLACK   = "\033[0;30m";
    public static final String RED     = "\033[0;31m";
    public static final String GREEN   = "\033[0;32m";
    public static final String YELLOW  = "\033[0;33m";
    public static final String BLUE    = "\033[0;34m";
    public static final String PURPLE  = "\033[0;35m";
    public static final String CYAN    = "\033[0;36m";
    public static final String WHITE   = "\033[0;37m";

    // ─────────────────────────────────────────────────────────────
    //  Bold Colours
    // ─────────────────────────────────────────────────────────────

    public static final String BOLD_BLACK   = "\033[1;30m";
    public static final String BOLD_RED     = "\033[1;31m";
    public static final String BOLD_GREEN   = "\033[1;32m";
    public static final String BOLD_YELLOW  = "\033[1;33m";
    public static final String BOLD_BLUE    = "\033[1;34m";
    public static final String BOLD_PURPLE  = "\033[1;35m";
    public static final String BOLD_CYAN    = "\033[1;36m";
    public static final String BOLD_WHITE   = "\033[1;37m";

    // ─────────────────────────────────────────────────────────────
    //  Semantic Aliases (preferred for readability in UI code)
    // ─────────────────────────────────────────────────────────────

    /** Used for section headers and the UniHive logo. */
    public static final String HEADER  = BOLD_CYAN;

    /** Used for success confirmation messages. */
    public static final String SUCCESS = BOLD_GREEN;

    /** Used for validation errors and login failures. */
    public static final String ERROR   = BOLD_RED;

    /** Used for prompts and informational messages. */
    public static final String INFO    = YELLOW;

    /** Used for incoming chat messages from other users. */
    public static final String CHAT_OTHER = CYAN;

    /** Used for the logged-in user's own echoed messages. */
    public static final String CHAT_SELF  = GREEN;

    /** Used for system join/leave notifications. */
    public static final String SYSTEM_MSG = PURPLE;

    // ─────────────────────────────────────────────────────────────
    //  Constructor — prevent instantiation
    // ─────────────────────────────────────────────────────────────

    private ConsoleColors() {
        throw new UnsupportedOperationException("Utility class — do not instantiate.");
    }
}
