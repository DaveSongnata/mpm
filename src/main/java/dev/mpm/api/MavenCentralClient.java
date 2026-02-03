package dev.mpm.api;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client for Maven Central Search API.
 * Uses only Java standard library (no external JSON dependencies).
 */
public class MavenCentralClient {

    private static final String SEARCH_URL = "https://search.maven.org/solrsearch/select";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Represents a Maven artifact from search results.
     */
    public static class Artifact {
        public final String groupId;
        public final String artifactId;
        public final String latestVersion;
        public final int versionCount;

        public Artifact(String groupId, String artifactId, String latestVersion, int versionCount) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.latestVersion = latestVersion;
            this.versionCount = versionCount;
        }

        public String getCoordinates() {
            return groupId + ":" + artifactId + ":" + latestVersion;
        }

        @Override
        public String toString() {
            return groupId + ":" + artifactId + "@" + latestVersion;
        }
    }

    /**
     * Searches for artifacts by name.
     *
     * @param query the search query (artifact name or partial name)
     * @param rows  maximum number of results
     * @return list of matching artifacts, sorted by popularity (versionCount)
     */
    public List<Artifact> search(String query, int rows) throws IOException, InterruptedException {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = SEARCH_URL + "?q=" + encodedQuery + "&rows=" + rows + "&wt=json";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("API returned status " + response.statusCode());
        }

        return parseSearchResponse(response.body());
    }

    /**
     * Searches for a specific artifact by groupId and artifactId.
     *
     * @param groupId    the group ID
     * @param artifactId the artifact ID
     * @return the artifact if found, null otherwise
     */
    public Artifact searchExact(String groupId, String artifactId) throws IOException, InterruptedException {
        String query = "g:" + URLEncoder.encode(groupId, StandardCharsets.UTF_8) +
                " AND a:" + URLEncoder.encode(artifactId, StandardCharsets.UTF_8);
        String url = SEARCH_URL + "?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&rows=1&wt=json";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("API returned status " + response.statusCode());
        }

        List<Artifact> results = parseSearchResponse(response.body());
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Gets all versions of a specific artifact.
     *
     * @param groupId    the group ID
     * @param artifactId the artifact ID
     * @return list of available versions (newest first)
     */
    public List<String> getVersions(String groupId, String artifactId) throws IOException, InterruptedException {
        String query = "g:" + URLEncoder.encode(groupId, StandardCharsets.UTF_8) +
                " AND a:" + URLEncoder.encode(artifactId, StandardCharsets.UTF_8);
        String url = SEARCH_URL + "?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8) +
                "&core=gav&rows=100&wt=json";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("API returned status " + response.statusCode());
        }

        return parseVersionsResponse(response.body());
    }

    /**
     * Parses the search response JSON without external libraries.
     * Uses regex-based parsing for the simple JSON structure.
     */
    private List<Artifact> parseSearchResponse(String json) {
        List<Artifact> artifacts = new ArrayList<>();

        // Find all docs in the response
        Pattern docPattern = Pattern.compile("\\{[^{}]*\"g\"\\s*:\\s*\"([^\"]+)\"[^{}]*\"a\"\\s*:\\s*\"([^\"]+)\"[^{}]*\\}");
        Matcher docMatcher = docPattern.matcher(json);

        while (docMatcher.find()) {
            String docJson = docMatcher.group(0);

            String groupId = extractField(docJson, "g");
            String artifactId = extractField(docJson, "a");
            String latestVersion = extractField(docJson, "latestVersion");
            int versionCount = extractIntField(docJson, "versionCount");

            if (groupId != null && artifactId != null && latestVersion != null) {
                artifacts.add(new Artifact(groupId, artifactId, latestVersion, versionCount));
            }
        }

        // Sort by versionCount descending (more versions = more likely official)
        artifacts.sort(Comparator.comparingInt((Artifact a) -> a.versionCount).reversed());

        return artifacts;
    }

    /**
     * Parses the versions response JSON.
     */
    private List<String> parseVersionsResponse(String json) {
        List<String> versions = new ArrayList<>();

        Pattern versionPattern = Pattern.compile("\"v\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = versionPattern.matcher(json);

        while (matcher.find()) {
            versions.add(matcher.group(1));
        }

        return versions;
    }

    /**
     * Extracts a string field from JSON.
     */
    private String extractField(String json, String field) {
        Pattern pattern = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * Extracts an integer field from JSON.
     */
    private int extractIntField(String json, String field) {
        Pattern pattern = Pattern.compile("\"" + field + "\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }
}
