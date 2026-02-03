package dev.mpm.command;

import dev.mpm.pom.PomEditor;
import dev.mpm.pom.PomEditor.Dependency;
import dev.mpm.util.Console;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Lists dependencies in the current project.
 *
 * Usage:
 *   mpm list         - lists all dependencies
 *   mpm list --tree  - shows dependency tree (requires mvn)
 */
public class ListCommand implements Command {

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "List project dependencies";
    }

    @Override
    public String getUsage() {
        return "mpm list";
    }

    @Override
    public int execute(String[] args) {
        // Check if pom.xml exists
        PomEditor pom = new PomEditor(Path.of("pom.xml"));
        if (!pom.exists()) {
            Console.error("pom.xml not found in current directory");
            Console.info("Run 'mpm init' to create a new project");
            return 1;
        }

        try {
            List<Dependency> dependencies = pom.getDependencies();

            if (dependencies.isEmpty()) {
                Console.info("No dependencies found");
                Console.println("Run 'mpm install <artifact>' to add dependencies");
                return 0;
            }

            Console.println(Console.bold("Dependencies (" + dependencies.size() + "):"));
            Console.println();

            // Group by scope
            printByScope(dependencies, "compile", "Compile");
            printByScope(dependencies, "test", "Test");
            printByScope(dependencies, "provided", "Provided");
            printByScope(dependencies, "runtime", "Runtime");
            printByScope(dependencies, null, "Default"); // null scope

            return 0;

        } catch (IOException e) {
            Console.error("Error reading pom.xml: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Prints dependencies with a specific scope.
     */
    private void printByScope(List<Dependency> dependencies, String scope, String label) {
        boolean headerPrinted = false;

        for (Dependency dep : dependencies) {
            String depScope = dep.scope;

            // Match scope (null scope is treated as "compile")
            boolean matches;
            if (scope == null) {
                matches = depScope == null;
            } else if (scope.equals("compile")) {
                matches = depScope == null || depScope.equals("compile");
            } else {
                matches = scope.equals(depScope);
            }

            if (matches) {
                if (!headerPrinted) {
                    Console.println(Console.dim("  " + label + ":"));
                    headerPrinted = true;
                }

                String versionStr = dep.version != null ? "@" + dep.version : "";
                Console.println("    " + dep.groupId + ":" + Console.bold(dep.artifactId) + Console.green(versionStr));
            }
        }

        if (headerPrinted) {
            Console.println();
        }
    }
}
