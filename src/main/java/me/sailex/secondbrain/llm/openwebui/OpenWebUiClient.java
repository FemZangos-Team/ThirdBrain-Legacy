package me.sailex.secondbrain.llm.openwebui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.sailex.secondbrain.exception.LLMServiceException;
import me.sailex.secondbrain.history.Message;
import me.sailex.secondbrain.llm.LLMClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class OpenWebUiClient implements LLMClient {

    private static final String CHAT_PATH = "/api/chat/completions";
    private static final String MODELS_PATH = "/api/models";
    private static final String DEFAULT_BASE_URL = "http://localhost:3000";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient client;
    private final String model;
    private final String apiKey;
    private final String normalizedBaseUrl;
    private final Duration requestTimeout;

    public OpenWebUiClient(String model, String apiKey, String baseUrl, int timeout) {
        this.model = model;
        this.apiKey = apiKey;
        this.normalizedBaseUrl = normalizeBaseUrl(baseUrl);
        this.requestTimeout = Duration.ofSeconds(Math.max(timeout, 1));
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(this.requestTimeout)
                .build();
    }

    @Override
    public Message chat(List<Message> messages) {
        return chat(messages, List.of());
    }

    @Override
    public Message chat(List<Message> messages, List<String> collectionIds) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizedBaseUrl + CHAT_PATH))
                    .timeout(requestTimeout)
                    .header("Content-Type", "application/json")
                    .headers(authHeaders())
                    .POST(HttpRequest.BodyPublishers.ofString(buildChatPayload(messages, collectionIds), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = sendRequest(request);
            JsonNode root = MAPPER.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            return new Message(content, "assistant");
        } catch (Exception e) {
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            String safeRootMessage = root.getMessage();
            if (safeRootMessage == null || safeRootMessage.isBlank()) {
                safeRootMessage = root.getClass().getSimpleName();
            }
            String prompt = messages.isEmpty() ? "" : messages.get(messages.size() - 1).getMessage();
            throw new LLMServiceException("OpenWebUI error at " + normalizedBaseUrl + CHAT_PATH + "\n" + root.getClass().getSimpleName() + ": " + safeRootMessage + "\nPrompt: " + truncate(prompt, 100), e);
        }
    }

    @Override
    public void checkServiceIsReachable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizedBaseUrl + MODELS_PATH))
                    .timeout(requestTimeout)
                    .headers(authHeaders())
                    .GET()
                    .build();
            sendRequest(request);
        } catch (Exception e) {
            throw new LLMServiceException("OpenWebUI server is not reachable at: " + normalizedBaseUrl, e);
        }
    }

    @Override
    public void stopService() {
        // nothing to stop
    }

    private String buildChatPayload(List<Message> messages, List<String> collectionIds) throws IOException {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", model == null ? "" : model);
        body.put("stream", false);
        ArrayNode messageArray = body.putArray("messages");
        for (Message message : messages) {
            ObjectNode messageNode = messageArray.addObject();
            messageNode.put("role", normalizeRole(message == null ? null : message.getRole()));
            messageNode.put("content", message == null || message.getMessage() == null ? "" : message.getMessage());
        }
        List<String> sanitizedCollectionIds = sanitizeCollectionIds(collectionIds);
        if (!sanitizedCollectionIds.isEmpty()) {
            ArrayNode files = body.putArray("files");
            for (String collectionId : sanitizedCollectionIds) {
                ObjectNode file = files.addObject();
                file.put("type", "collection");
                file.put("id", collectionId);
            }
        }
        return MAPPER.writeValueAsString(body);
    }

    private static List<String> sanitizeCollectionIds(List<String> collectionIds) {
        if (collectionIds == null || collectionIds.isEmpty()) {
            return List.of();
        }
        Set<String> deduplicated = new LinkedHashSet<>();
        for (String id : collectionIds) {
            if (id == null) {
                continue;
            }
            String trimmed = id.trim();
            if (!trimmed.isEmpty()) {
                deduplicated.add(trimmed);
            }
        }
        return deduplicated.stream().toList();
    }

    private HttpResponse<String> sendRequest(HttpRequest request) throws IOException {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                String body = response.body();
                String errorMsg = "OpenWebUI request failed with HTTP " + status;
                if (body != null && !body.isBlank()) {
                    errorMsg += ": " + truncate(body, 500);
                }
                throw new LLMServiceException(errorMsg);
            }
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }

    private String[] authHeaders() {
        if (apiKey == null || apiKey.isBlank()) {
            return new String[0];
        }
        return new String[]{"Authorization", "Bearer " + apiKey.trim()};
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return DEFAULT_BASE_URL;
        }
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "user";
        }
        String normalized = role.trim().toLowerCase();
        if ("system".equals(normalized) || "assistant".equals(normalized)) {
            return normalized;
        }
        return "user";
    }
}