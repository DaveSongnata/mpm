package dev.mpm;

import dev.mpm.command.*;
import dev.mpm.util.Console;

import java.util.HashMap;
import java.util.Map;

/**
 * Maven Package Manager (mpm) - npm-like CLI for Maven dependencies.
 *
 * Usage:
 *   mpm <command> [arguments]
 *
 * Commands:
 *   install  - Install a dependency
 *   search   - Search for artifacts
 *   remove   - Remove a dependency
 *   init     - Initialize a new project
 *   list     - List dependencies
 *   help     - Show help
 *   version  - Show version
 */
public class Mpm {

    private static final String VERSION = "1.0.0";

    private static final Map<String, Command> COMMANDS = new HashMap<>();

    static {
        registerCommand(new InstallCommand());
        registerCommand(new SearchCommand());
        registerCommand(new RemoveCommand());
        registerCommand(new InitCommand());
        registerCommand(new ListCommand());
    }

    private static void registerCommand(Command command) {
        COMMANDS.put(command.getName(), command);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(0);
        }

        String commandName = args[0].toLowerCase();

        // Handle special commands
        switch (commandName) {
            case "help":
            case "--help":
            case "-h":
                printUsage();
                System.exit(0);
                break;
            case "version":
            case "--version":
            case "-v":
                printVersion();
                System.exit(0);
                break;
        }

        // Handle aliases
        if (commandName.equals("i") || commandName.equals("add")) {
            commandName = "install";
        } else if (commandName.equals("rm") || commandName.equals("uninstall")) {
            commandName = "remove";
        } else if (commandName.equals("ls")) {
            commandName = "list";
        } else if (commandName.equals("s") || commandName.equals("find")) {
            commandName = "search";
        }

        // Find and execute command
        Command command = COMMANDS.get(commandName);
        if (command == null) {
            Console.error("Unknown command: " + commandName);
            Console.println();
            printUsage();
            System.exit(1);
        }

        // Extract command arguments (everything after the command name)
        String[] commandArgs = new String[args.length - 1];
        System.arraycopy(args, 1, commandArgs, 0, commandArgs.length);

        // Execute the command
        int exitCode = command.execute(commandArgs);
        System.exit(exitCode);
    }

    private static void printUsage() {
        Console.println(Console.bold("mpm") + " - Maven Package Manager v" + VERSION);
        Console.println();
        Console.println(Console.bold("Usage:"));
        Console.println("  mpm <command> [arguments]");
        Console.println();
        Console.println(Console.bold("Commands:"));

        for (Command cmd : COMMANDS.values()) {
            String name = String.format("  %-10s", cmd.getName());
            Console.println(Console.cyan(name) + cmd.getDescription());
        }

        Console.println(Console.cyan("  help      ") + "Show this help message");
        Console.println(Console.cyan("  version   ") + "Show version");

        Console.println();
        Console.println(Console.bold("Aliases:"));
        Console.println(Console.dim("  i, add        -> install"));
        Console.println(Console.dim("  rm, uninstall -> remove"));
        Console.println(Console.dim("  ls            -> list"));
        Console.println(Console.dim("  s, find       -> search"));

        Console.println();
        Console.println(Console.bold("Examples:"));
        Console.println("  mpm init                          Create a new pom.xml");
        Console.println("  mpm install lombok                Install lombok (latest)");
        Console.println("  mpm install jackson-databind@2.15.2  Install specific version");
        Console.println("  mpm install junit --scope test    Install with test scope");
        Console.println("  mpm search spring-boot            Search for artifacts");
        Console.println("  mpm list                          List dependencies");
        Console.println("  mpm remove lombok                 Remove a dependency");
    }

    private static void printVersion() {
        Console.println("mpm v" + VERSION);
    }
}
