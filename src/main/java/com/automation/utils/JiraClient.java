package com.automation.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

public class JiraClient {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String baseUrl;
    private final String email;
    private final String apiToken;
    private final String projectKey;
    private final HttpClient httpClient;
    private final String authorization;

    /**
     * Creates a Jira client using the required environment variables.
     *
     * @throws IllegalStateException if any Jira setting is missing
     */
    public JiraClient() {
        baseUrl = environmentValue("JIRA_BASE_URL").replaceAll("/+$", "");
        email = environmentValue("JIRA_EMAIL");
        apiToken = environmentValue("JIRA_API_TOKEN");
        projectKey = environmentValue("JIRA_PROJECT_KEY");
        httpClient = HttpClient.newHttpClient();
        authorization = "Basic " + Base64.getEncoder().encodeToString(
                (email + ":" + apiToken).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Checks whether all required Jira environment variables are set.
     *
     * @return {@code true} when Jira integration is fully configured
     */
    public static boolean isConfigured() {
        return hasValue("JIRA_BASE_URL")
                && hasValue("JIRA_EMAIL")
                && hasValue("JIRA_API_TOKEN")
                && hasValue("JIRA_PROJECT_KEY");
    }

    /**
     * Checks whether at least one Jira environment variable is set.
     *
     * @return {@code true} when Jira configuration is partial or complete
     */
    public static boolean hasPartialConfiguration() {
        return hasValue("JIRA_BASE_URL")
                || hasValue("JIRA_EMAIL")
                || hasValue("JIRA_API_TOKEN")
                || hasValue("JIRA_PROJECT_KEY");
    }

    /**
     * Checks whether an environment variable contains a non-blank value.
     *
     * @param name the environment variable name
     * @return {@code true} when the variable is set and non-blank
     */
    private static boolean hasValue(String name) {
        String value = System.getenv(name);
        return value != null && !value.isBlank();
    }

    /**
     * Reads a required environment variable.
     *
     * @param name the environment variable name
     * @return the trimmed environment variable value
     * @throws IllegalStateException if the variable is missing or blank
     */
    private static String environmentValue(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing Jira environment variable: " + name);
        }
        return value.trim();
    }

    /**
     * Reuses an existing Jira issue for the same failure or creates a new Bug issue.
     *
     * @param testName the failing test name
     * @param failureDetails the failure message used to identify duplicate failures
     * @param description the execution details for the issue description
     * @param timestamp the time the failure was observed
     * @return the Jira issue key
     * @throws IOException if a Jira request fails or returns invalid data
     * @throws InterruptedException if the Jira request is interrupted
     */
    public String createOrFindIssue(String testName, String failureDetails, String description,
                                    Instant timestamp) throws IOException, InterruptedException {
        String failureLabel = "failure-" + fingerprint(testName + "\n" + failureDetails);
        String existingIssue = findExistingIssue(failureLabel);
        if (existingIssue != null) {
            return existingIssue;
        }

        ObjectNode payload = OBJECT_MAPPER.createObjectNode();
        ObjectNode fields = payload.putObject("fields");
        fields.putObject("project").put("key", projectKey);
        fields.put("summary", "[Playwright] " + testName);
        fields.set("description", createDescription(description, timestamp));
        fields.putObject("issuetype").put("name", "Bug");
        ArrayNode labels = fields.putArray("labels");
        labels.add("automation-failure");
        labels.add(failureLabel);

        HttpRequest request = authorizedRequest("/rest/api/3/issue")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        OBJECT_MAPPER.writeValueAsString(payload), StandardCharsets.UTF_8))
                .build();
        JsonNode response = sendJson(request);
        JsonNode key = response.get("key");
        if (key == null || key.asText().isBlank()) {
            throw new IOException("Jira did not return an issue key after creating the failure issue");
        }
        return key.asText();
    }

    /**
     * Searches the configured project for an issue with the supplied failure label.
     *
     * @param failureLabel the unique label identifying the failure
     * @return the matching Jira issue key, or {@code null} if none exists
     * @throws IOException if the Jira response cannot be read
     * @throws InterruptedException if the Jira request is interrupted
     */
    private String findExistingIssue(String failureLabel) throws IOException, InterruptedException {
        String jql = "project = \"" + escapeJql(projectKey)
                + "\" AND labels = \"automation-failure\" AND labels = \""
                + failureLabel + "\"";
        String query = URLEncoder.encode(jql, StandardCharsets.UTF_8);
        HttpRequest request = authorizedRequest(
                "/rest/api/3/search/jql?jql=" + query + "&maxResults=1&fields=key")
                .GET()
                .build();
        JsonNode response = sendJson(request);
        JsonNode issues = response.path("issues");
        if (issues.isArray() && !issues.isEmpty()) {
            return issues.get(0).path("key").asText(null);
        }
        return null;
    }

    /**
     * Escapes characters that have special meaning in a JQL string literal.
     *
     * @param value the value to escape
     * @return the escaped value
     */
    private static String escapeJql(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Builds Jira's document-format description with the failure timestamp.
     *
     * @param description the execution details to include
     * @param timestamp the time the failure was observed
     * @return the Jira document-format description
     */
    private ObjectNode createDescription(String description, Instant timestamp) {
        ObjectNode document = OBJECT_MAPPER.createObjectNode();
        document.put("type", "doc");
        document.put("version", 1);
        ArrayNode paragraphs = document.putArray("content");
        for (String line : description.split("\\R", -1)) {
            ObjectNode paragraph = paragraphs.addObject();
            paragraph.put("type", "paragraph");
            paragraph.putArray("content").addObject()
                    .put("type", "text")
                    .put("text", line);
        }
        ObjectNode timestampParagraph = paragraphs.addObject();
        timestampParagraph.put("type", "paragraph");
        timestampParagraph.putArray("content").addObject()
                .put("type", "text")
                .put("text", "Timestamp: " + timestamp);
        return document;
    }

    /**
     * Uploads a PNG screenshot to a Jira issue.
     *
     * @param issueKey the Jira issue key
     * @param screenshot the screenshot file to upload
     * @throws IOException if the file cannot be read or Jira rejects the request
     * @throws InterruptedException if the Jira request is interrupted
     */
    public void attachScreenshot(String issueKey, Path screenshot)
            throws IOException, InterruptedException {
        String boundary = "----PlaywrightBoundary" + System.nanoTime();
        String fileName = screenshot.getFileName().toString();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(Files.readAllBytes(screenshot));
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest request = authorizedRequest("/rest/api/3/issue/" + issueKey + "/attachments")
                .header("X-Atlassian-Token", "no-check")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();
        send(request);
    }

    /**
     * Creates a Jira API request builder with common authorization and response headers.
     *
     * @param path the Jira API path
     * @return the configured request builder
     */
    private HttpRequest.Builder authorizedRequest(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Authorization", authorization)
                .header("Accept", "application/json");
    }

    /**
     * Sends a request and parses its response body as JSON.
     *
     * @param request the Jira API request
     * @return the parsed JSON response
     * @throws IOException if the request fails or the response is invalid JSON
     * @throws InterruptedException if the Jira request is interrupted
     */
    private JsonNode sendJson(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = send(request);
        try {
            return OBJECT_MAPPER.readTree(response.body());
        } catch (IOException exception) {
            throw new IOException("Jira returned invalid JSON for " + request.uri(), exception);
        }
    }

    /**
     * Sends a Jira API request and rejects non-success HTTP responses.
     *
     * @param request the request to send
     * @return the HTTP response
     * @throws IOException if Jira returns a non-success status
     * @throws InterruptedException if the request is interrupted
     */
    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Jira request failed with HTTP " + response.statusCode()
                    + ": " + response.body());
        }
        return response;
    }

    /**
     * Creates a short deterministic identifier for a failure string.
     *
     * @param value the failure details to hash
     * @return the first eight bytes of the SHA-256 hash as hexadecimal text
     * @throws IllegalStateException if SHA-256 is unavailable
     */
    private static String fingerprint(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (int index = 0; index < 8; index++) {
                result.append(String.format("%02x", digest[index]));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
