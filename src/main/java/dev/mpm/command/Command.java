package dev.mpm.command;

/**
 * Base interface for all mpm commands.
 */
public interface Command {

    /**
     * Gets the command name (e.g., "install", "search").
     */
    String getName();

    /**
     * Gets the command description for help text.
     */
    String getDescription();

    /**
     * Gets the usage pattern for help text.
     */
    String getUsage();

    /**
     * Executes the command.
     *
     * @param args the command arguments (excluding the command name itself)
     * @return exit code (0 = success, non-zero = error)
     */
    int execute(String[] args);
}
