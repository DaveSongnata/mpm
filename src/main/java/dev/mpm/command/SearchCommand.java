package dev.mpm.command;

import dev.mpm.api.MavenCentralClient;
import dev.mpm.api.MavenCentralClient.Artifact;
import dev.mpm.util.Console;

import java.io.IOException;
import java.util.List;

/**
 * Searches for Maven artifacts.
 *
 * Usage:
 *   mpm search <query>           - searches for artifacts
 *   mpm search <query> --limit 5 - limits results
 */
public class SearchCommand implements Command {

    private final MavenCentralClient client = new MavenCentralClient();

    @Override
    public String getName() {
        return "search";
    }

    @Override
    public String getDescription() {
        return "Search for Maven artifacts";
    }

    @Override
    public String getUsage() {
        return "mpm search <query> [--limit <n>]";
    }

    @Override
    public int execute(String[] args) {
        if (args.length == 0) {
            Console.error("Missing search query");
            Console.println("Usage: " + getUsage());
            return 1;
        }

        String query = args[0];
        int limit = 10;

        // Parse optional arguments
        for (int i = 1; i < args.length; i++) {
            if ("--limit".equals(args[i]) && i + 1 < args.length) {
                try {
                    limit = Integer.parseInt(args[++i]);
                } catch (NumberFormatException e) {
                    Console.error("Invalid limit value");
                    return 1;
                }
            }
        }

        try {
            Console.info("Searching for " + Console.bold(query) + "...");
            Console.println();

            List<Artifact> results = client.search(query, limit);

            if (results.isEmpty()) {
                Console.warn("No artifacts found matching: " + query);
                return 0;
            }

            Console.println(Console.bold("Found " + results.size() + " artifact(s):"));
            Console.println();

            for (int i = 0; i < results.size(); i++) {
                Artifact artifact = results.get(i);
                String number = String.format("%2d.", i + 1);

                Console.println(Console.dim(number) + " " + Console.bold(artifact.groupId + ":" + artifact.artifactId));
                Console.println("     Latest: " + Console.green(artifact.latestVersion) +
                        Console.dim(" (" + artifact.versionCount + " versions)"));

                // Show install command hint for first result
                if (i == 0) {
                    Console.println("     Install: " + Console.cyan("mpm install " + artifact.groupId + ":" + artifact.artifactId));
                }
                Console.println();
            }

            return 0;

        } catch (IOException e) {
            Console.error("Search failed: " + e.getMessage());
            return 1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Console.error("Search interrupted");
            return 1;
        }
    }
}
