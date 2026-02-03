package dev.mpm.command;

import dev.mpm.api.MavenCentralClient;
import dev.mpm.api.MavenCentralClient.Artifact;
import dev.mpm.pom.PomEditor;
import dev.mpm.util.Console;
import dev.mpm.util.MavenExecutor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Installs a Maven dependency.
 *
 * Usage:
 *   mpm install <artifact>              - installs latest version
 *   mpm install <artifact>@<version>    - installs specific version
 *   mpm install <g:a:v>                 - installs with full coordinates
 *   mpm install <artifact> --scope test - installs with specific scope
 */
public class InstallCommand implements Command {

    private final MavenCentralClient client = new MavenCentralClient();

    @Override
    public String getName() {
        return "install";
    }

    @Override
    public String getDescription() {
        return "Install a Maven dependency";
    }

    @Override
    public String getUsage() {
        return "mpm install <artifact> [--scope <scope>]";
    }

    @Override
    public int execute(String[] args) {
        if (args.length == 0) {
            Console.error("Missing artifact name");
            Console.println("Usage: " + getUsage());
            return 1;
        }

        // Parse arguments
        String artifactArg = args[0];
        String scope = "compile"; // default scope

        for (int i = 1; i < args.length; i++) {
            if ("--scope".equals(args[i]) && i + 1 < args.length) {
                scope = args[++i];
            }
        }

        // Validate scope
        if (!isValidScope(scope)) {
            Console.error("Invalid scope: " + scope);
            Console.println("Valid scopes: compile, test, provided, runtime, system, import");
            return 1;
        }

        // Check if pom.xml exists
        PomEditor pom = new PomEditor(Path.of("pom.xml"));
        if (!pom.exists()) {
            Console.error("pom.xml not found in current directory");
            Console.info("Run 'mpm init' to create a new project");
            return 1;
        }

        try {
            // Parse artifact specification
            ArtifactSpec spec = parseArtifactSpec(artifactArg);

            // If we don't have full coordinates, search for the artifact
            if (spec.groupId == null) {
                Console.info("Searching for " + Console.bold(spec.artifactId) + "...");

                List<Artifact> results = client.search(spec.artifactId, 10);
                if (results.isEmpty()) {
                    Console.error("No artifacts found matching: " + spec.artifactId);
                    return 1;
                }

                // If multiple results and the first one doesn't match exactly, show options
                Artifact selected = selectArtifact(results, spec.artifactId);
                if (selected == null) {
                    return 1;
                }

                spec.groupId = selected.groupId;
                spec.artifactId = selected.artifactId;
                if (spec.version == null) {
                    spec.version = selected.latestVersion;
                }
            }

            // If version is still null, get the latest version
            if (spec.version == null) {
                Artifact artifact = client.searchExact(spec.groupId, spec.artifactId);
                if (artifact == null) {
                    Console.error("Artifact not found: " + spec.groupId + ":" + spec.artifactId);
                    return 1;
                }
                spec.version = artifact.latestVersion;
            }

            // Check if dependency already exists
            pom.load();
            if (pom.hasDependency(spec.groupId, spec.artifactId)) {
                Console.warn("Dependency already exists: " + spec.groupId + ":" + spec.artifactId);
                Console.info("Use 'mpm remove' to remove it first, or edit pom.xml manually");
                return 0;
            }

            // Add the dependency
            Console.info("Installing " + Console.bold(spec.groupId + ":" + spec.artifactId + "@" + spec.version) +
                    (scope.equals("compile") ? "" : " (" + scope + ")"));

            boolean added = pom.addDependency(spec.groupId, spec.artifactId, spec.version, scope);
            if (added) {
                pom.save();
                Console.success("Added to pom.xml");

                // Resolve dependencies
                Console.info("Downloading dependencies...");
                boolean resolved = MavenExecutor.resolveDependencies(Path.of(".").toFile());

                if (resolved) {
                    Console.success("Installed " + spec.groupId + ":" + spec.artifactId + "@" + spec.version);
                    return 0;
                } else {
                    Console.warn("Dependencies added to pom.xml but Maven resolve failed");
                    Console.info("Try running 'mvn dependency:resolve' manually");
                    return 1;
                }
            } else {
                Console.error("Failed to add dependency");
                return 1;
            }

        } catch (IOException e) {
            Console.error("Error: " + e.getMessage());
            return 1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Console.error("Operation interrupted");
            return 1;
        }
    }

    /**
     * Parses an artifact specification.
     * Supports formats:
     *   - artifact
     *   - artifact@version
     *   - groupId:artifactId
     *   - groupId:artifactId:version
     */
    private ArtifactSpec parseArtifactSpec(String input) {
        ArtifactSpec spec = new ArtifactSpec();

        // Check for @ version separator (npm style)
        int atIndex = input.lastIndexOf('@');
        if (atIndex > 0) {
            spec.version = input.substring(atIndex + 1);
            input = input.substring(0, atIndex);
        }

        // Check for : coordinate separator (Maven style)
        String[] parts = input.split(":");
        if (parts.length == 1) {
            // Just artifact name
            spec.artifactId = parts[0];
        } else if (parts.length == 2) {
            // groupId:artifactId
            spec.groupId = parts[0];
            spec.artifactId = parts[1];
        } else if (parts.length >= 3) {
            // groupId:artifactId:version
            spec.groupId = parts[0];
            spec.artifactId = parts[1];
            if (spec.version == null) {
                spec.version = parts[2];
            }
        }

        return spec;
    }

    /**
     * Selects the most appropriate artifact from search results.
     */
    private Artifact selectArtifact(List<Artifact> results, String query) {
        // If top result matches the query exactly by artifactId, use it
        Artifact topResult = results.get(0);
        if (topResult.artifactId.equalsIgnoreCase(query)) {
            return topResult;
        }

        // Look for exact artifactId match
        for (Artifact artifact : results) {
            if (artifact.artifactId.equalsIgnoreCase(query)) {
                return artifact;
            }
        }

        // Show options to user
        Console.warn("Multiple artifacts found. The most popular is:");
        Console.println("  " + Console.bold(topResult.groupId + ":" + topResult.artifactId) +
                " (" + topResult.versionCount + " versions)");
        Console.println();
        Console.info("If this is not the right one, use the full coordinates:");
        Console.println("  mpm install " + Console.cyan("<groupId>:<artifactId>"));
        Console.println();
        Console.println("Other matches:");
        for (int i = 1; i < Math.min(results.size(), 5); i++) {
            Artifact a = results.get(i);
            Console.println("  " + Console.dim(a.groupId + ":" + a.artifactId + " (" + a.versionCount + " versions)"));
        }

        return topResult;
    }

    private boolean isValidScope(String scope) {
        return scope.equals("compile") ||
                scope.equals("test") ||
                scope.equals("provided") ||
                scope.equals("runtime") ||
                scope.equals("system") ||
                scope.equals("import");
    }

    /**
     * Internal class to hold parsed artifact specification.
     */
    private static class ArtifactSpec {
        String groupId;
        String artifactId;
        String version;
    }
}
