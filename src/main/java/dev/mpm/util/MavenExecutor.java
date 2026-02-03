package dev.mpm.util;

import java.io.File;
import java.io.IOException;

/**
 * Executes Maven commands as subprocess.
 */
public final class MavenExecutor {

    private MavenExecutor() {}

    /**
     * Resolves dependencies by running 'mvn dependency:resolve'.
     * This downloads all JARs to the local .m2 repository.
     *
     * @param workingDir the directory containing pom.xml
     * @return true if successful, false otherwise
     */
    public static boolean resolveDependencies(File workingDir) {
        return execute(workingDir, "dependency:resolve", "-q");
    }

    /**
     * Executes a Maven goal.
     *
     * @param workingDir the directory containing pom.xml
     * @param goals      the Maven goals to execute
     * @return true if successful, false otherwise
     */
    public static boolean execute(File workingDir, String... goals) {
        String mvnCommand = getMvnCommand();
        String[] command = new String[goals.length + 1];
        command[0] = mvnCommand;
        System.arraycopy(goals, 0, command, 1, goals.length);

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(workingDir);
            pb.inheritIO();

            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException e) {
            Console.error("Failed to execute Maven: " + e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Console.error("Maven execution interrupted");
            return false;
        }
    }

    /**
     * Checks if Maven is available on the system.
     *
     * @return true if Maven is available
     */
    public static boolean isMavenAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(getMvnCommand(), "-v");
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    /**
     * Gets the correct Maven command for the current OS.
     */
    private static String getMvnCommand() {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        return isWindows ? "mvn.cmd" : "mvn";
    }
}
