package dev.mpm.command;

import dev.mpm.pom.PomEditor;
import dev.mpm.pom.PomEditor.Dependency;
import dev.mpm.util.Console;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Removes a Maven dependency from pom.xml.
 *
 * Usage:
 *   mpm remove <artifact>       - removes by artifact name
 *   mpm remove <groupId:artifact> - removes by coordinates
 */
public class RemoveCommand implements Command {

    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public String getDescription() {
        return "Remove a Maven dependency";
    }

    @Override
    public String getUsage() {
        return "mpm remove <artifact|groupId:artifact>";
    }

    @Override
    public int execute(String[] args) {
        if (args.length == 0) {
            Console.error("Missing artifact name");
            Console.println("Usage: " + getUsage());
            return 1;
        }

        String artifactArg = args[0];

        // Check if pom.xml exists
        PomEditor pom = new PomEditor(Path.of("pom.xml"));
        if (!pom.exists()) {
            Console.error("pom.xml not found in current directory");
            return 1;
        }

        try {
            pom.load();
            List<Dependency> dependencies = pom.getDependencies();

            // Parse artifact specification
            String groupId = null;
            String artifactId;

            if (artifactArg.contains(":")) {
                String[] parts = artifactArg.split(":");
                groupId = parts[0];
                artifactId = parts[1];
            } else {
                artifactId = artifactArg;
            }

            // Find matching dependency
            Dependency toRemove = null;
            int matchCount = 0;

            for (Dependency dep : dependencies) {
                boolean artifactMatch = dep.artifactId.equals(artifactId);
                boolean groupMatch = groupId == null || dep.groupId.equals(groupId);

                if (artifactMatch && groupMatch) {
                    toRemove = dep;
                    matchCount++;
                }
            }

            if (matchCount == 0) {
                Console.error("Dependency not found: " + artifactArg);
                Console.info("Use 'mpm list' to see installed dependencies");
                return 1;
            }

            if (matchCount > 1) {
                Console.error("Multiple dependencies match '" + artifactId + "'");
                Console.info("Please specify the full coordinates: mpm remove <groupId>:" + artifactId);

                for (Dependency dep : dependencies) {
                    if (dep.artifactId.equals(artifactId)) {
                        Console.println("  - " + dep.groupId + ":" + dep.artifactId);
                    }
                }
                return 1;
            }

            // Remove the dependency
            Console.info("Removing " + Console.bold(toRemove.groupId + ":" + toRemove.artifactId) + "...");

            boolean removed = pom.removeDependency(toRemove.groupId, toRemove.artifactId);
            if (removed) {
                pom.save();
                Console.success("Removed " + toRemove.groupId + ":" + toRemove.artifactId);
                return 0;
            } else {
                Console.error("Failed to remove dependency");
                return 1;
            }

        } catch (IOException e) {
            Console.error("Error: " + e.getMessage());
            return 1;
        }
    }
}
