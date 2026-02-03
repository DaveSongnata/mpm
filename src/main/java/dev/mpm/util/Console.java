package dev.mpm.util;

/**
 * Utility class for console output with ANSI colors.
 */
public final class Console {

    // ANSI color codes
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";

    private static boolean colorsEnabled = true;

    static {
        // Disable colors on Windows CMD (no ANSI support by default)
        String term = System.getenv("TERM");
        String conEmuAnsi = System.getenv("ConEmuANSI");
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            // Enable if running in Windows Terminal, ConEmu, or similar
            colorsEnabled = term != null || "ON".equals(conEmuAnsi) || System.getenv("WT_SESSION") != null;
        }
    }

    private Console() {}

    public static void success(String message) {
        println(GREEN + "+" + RESET + " " + message);
    }

    public static void error(String message) {
        println(RED + "x" + RESET + " " + message);
    }

    public static void warn(String message) {
        println(YELLOW + "!" + RESET + " " + message);
    }

    public static void info(String message) {
        println(CYAN + "i" + RESET + " " + message);
    }

    public static void print(String message) {
        System.out.print(colorsEnabled ? message : stripAnsi(message));
    }

    public static void println(String message) {
        System.out.println(colorsEnabled ? message : stripAnsi(message));
    }

    public static void println() {
        System.out.println();
    }

    public static String bold(String text) {
        return BOLD + text + RESET;
    }

    public static String dim(String text) {
        return DIM + text + RESET;
    }

    public static String green(String text) {
        return GREEN + text + RESET;
    }

    public static String red(String text) {
        return RED + text + RESET;
    }

    public static String yellow(String text) {
        return YELLOW + text + RESET;
    }

    public static String cyan(String text) {
        return CYAN + text + RESET;
    }

    private static String stripAnsi(String text) {
        return text.replaceAll("\u001B\\[[;\\d]*m", "");
    }
}
