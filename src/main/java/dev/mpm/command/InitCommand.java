package dev.mpm.command;

import dev.mpm.pom.PomEditor;
import dev.mpm.util.Console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Initializes a new Maven project with a basic pom.xml.
 *
 * Usage:
 *   mpm init                    - interactive mode
 *   mpm init --yes              - use defaults
 *   mpm init --groupId com.example --artifactId myapp --version 1.0.0
 */
public class InitCommand implements Command {

    @Override
    public String getName() {
        return "init";
    }

    @Override
    public String getDescription() {
        return "Initialize a new Maven project";
    }

    @Override
    public String getUsage() {
        return "mpm init [--yes] [--groupId <g>] [--artifactId <a>] [--version <v>]";
    }

    @Override
    public int execute(String[] args) {
        Path pomPath = Path.of("pom.xml");

        // Check if pom.xml already exists
        if (Files.exists(pomPath)) {
            Console.error("pom.xml already exists in current directory");
            Console.info("Use 'mpm install' to add dependencies");
            return 1;
        }

        // Parse arguments
        String groupId = null;
        String artifactId = null;
        String version = null;
        boolean useDefaults = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--yes":
                case "-y":
                    useDefaults = true;
                    break;
                case "--groupId":
                case "-g":
                    if (i + 1 < args.length) groupId = args[++i];
                    break;
                case "--artifactId":
                case "-a":
                    if (i + 1 < args.length) artifactId = args[++i];
                    break;
                case "--version":
                case "-v":
                    if (i + 1 < args.length) version = args[++i];
                    break;
            }
        }

        // Get current directory name for defaults
        String currentDir = Path.of(".").toAbsolutePath().getParent().getFileName().toString();
        String defaultGroupId = "com.example";
        String defaultArtifactId = sanitizeArtifactId(currentDir);
        String defaultVersion = "1.0.0-SNAPSHOT";

        try {
            if (useDefaults) {
                // Use defaults or provided values
                groupId = groupId != null ? groupId : defaultGroupId;
                artifactId = artifactId != null ? artifactId : defaultArtifactId;
                version = version != null ? version : defaultVersion;
            } else {
                // Interactive mode
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

                Console.println(Console.bold("Initialize new Maven project"));
                Console.println();

                groupId = prompt(reader, "groupId", groupId, defaultGroupId);
                artifactId = prompt(reader, "artifactId", artifactId, defaultArtifactId);
                version = prompt(reader, "version", version, defaultVersion);

                Console.println();
            }

            // Create the pom.xml
            PomEditor pom = new PomEditor(pomPath);
            pom.createNew(groupId, artifactId, version);

            Console.success("Created pom.xml");
            Console.println();
            Console.println(Console.dim("  groupId:    ") + groupId);
            Console.println(Console.dim("  artifactId: ") + artifactId);
            Console.println(Console.dim("  version:    ") + version);
            Console.println();
            Console.info("Run 'mpm install <artifact>' to add dependencies");

            return 0;

        } catch (IOException e) {
            Console.error("Failed to create pom.xml: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Prompts the user for a value.
     */
    private String prompt(BufferedReader reader, String name, String currentValue, String defaultValue) throws IOException {
        if (currentValue != null) {
            return currentValue;
        }

        Console.print(name + " (" + defaultValue + "): ");
        String input = reader.readLine();

        if (input == null || input.trim().isEmpty()) {
            return defaultValue;
        }

        return input.trim();
    }

    /**
     * Sanitizes a directory name for use as an artifactId.
     */
    private String sanitizeArtifactId(String name) {
        // Replace non-alphanumeric characters with hyphens
        String sanitized = name.replaceAll("[^a-zA-Z0-9]", "-");

        // Remove leading/trailing hyphens
        sanitized = sanitized.replaceAll("^-+|-+$", "");

        // Convert to lowercase
        sanitized = sanitized.toLowerCase();

        // Ensure it starts with a letter
        if (sanitized.isEmpty() || !Character.isLetter(sanitized.charAt(0))) {
            sanitized = "app";
        }

        return sanitized;
    }
}
