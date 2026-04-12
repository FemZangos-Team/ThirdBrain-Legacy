package me.sailex.secondbrain.llm.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.sailex.secondbrain.exception.LLMServiceException;
import me.sailex.secondbrain.history.Message;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class OpenAiCompatibleChatClient {

    public enum ProviderTarget {
        OPENAI("https://api.openai.com/v1", CommandJsonMode.JSON_SCHEMA, "OpenAI"),
        OLLAMA("http://localhost:11434/v1", CommandJsonMode.JSON_OBJECT, "Ollama");

        private final String defaultBaseUrl;
        private final CommandJsonMode commandJsonMode;
        private final String providerName;

        ProviderTarget(String defaultBaseUrl, CommandJsonMode commandJsonMode, String providerName) {
            this.defaultBaseUrl = defaultBaseUrl;
            this.commandJsonMode = commandJsonMode;
            this.providerName = providerName;
        }
    }

    private enum CommandJsonMode {
        JSON_SCHEMA,
        JSON_OBJECT
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CHAT_PATH = "/chat/completions";
    private static final String MODELS_PATH = "/models";
    private static final String SAFE_PROVIDER_ERROR = "Provider returned an error. Check base URL, model, and API key.";

    private final HttpClient client;
    private final String model;
    private final String apiKey;
    private final String normalizedBaseUrl;
    private final Duration requestTimeout;
    private final ProviderTarget providerTarget;

    public OpenAiCompatibleChatClient(String model, String apiKey, String baseUrl, int timeout, ProviderTarget providerTarget) {
        this.model = model;
        this.apiKey = apiKey;
        this.providerTarget = providerTarget;
        this.normalizedBaseUrl = normalizeBaseUrl(baseUrl, providerTarget.defaultBaseUrl);
        this.requestTimeout = Duration.ofSeconds(Math.max(timeout, 1));
        this.client = HttpClient.newBuilder()
                .connectTimeout(this.requestTimeout)
                .build();
    }

    public Message chat(List<Message> messages) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizedBaseUrl + CHAT_PATH))
                    .timeout(requestTimeout)
                    .header("Content-Type", "application/json")
                    .headers(authHeaders())
                    .POST(HttpRequest.BodyPublishers.ofString(buildChatPayload(messages), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = sendRequest(request);
            JsonNode root = MAPPER.readTree(response.body());
            return new Message(extractContent(root), "assistant");
        } catch (Exception e) {
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            String prompt = messages.isEmpty() ? "" : messages.get(messages.size() - 1).getMessage();
            throw new LLMServiceException("Could not generate Response for prompt: " + prompt
                    + "\nRoot cause: " + root.getClass().getSimpleName() + ": " + SAFE_PROVIDER_ERROR, e);
        }
    }

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
            throw new LLMServiceException(providerTarget.providerName + " server is not reachable at: " + normalizedBaseUrl, e);
        }
    }

    public String getNormalizedBaseUrl() {
        return normalizedBaseUrl;
    }

    private String buildChatPayload(List<Message> messages) throws IOException {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", model == null ? "" : model);
        body.put("stream", false);

        ArrayNode messageArray = body.putArray("messages");
        for (Message message : messages) {
            ObjectNode messageNode = messageArray.addObject();
            messageNode.put("role", normalizeRole(message == null ? null : message.getRole()));
            messageNode.put("content", message == null || message.getMessage() == null ? "" : message.getMessage());
        }

        if (shouldForceCommandJson(messages)) {
            applyResponseFormat(body);
        }

        return MAPPER.writeValueAsString(body);
    }

    private void applyResponseFormat(ObjectNode body) {
        ObjectNode responseFormat = body.putObject("response_format");
        if (providerTarget.commandJsonMode == CommandJsonMode.JSON_SCHEMA) {
            responseFormat.put("type", "json_schema");
            ObjectNode jsonSchema = responseFormat.putObject("json_schema");
            jsonSchema.put("name", "npc_command_message");
            jsonSchema.put("strict", true);

            ObjectNode schema = jsonSchema.putObject("schema");
            schema.put("type", "object");
            schema.put("additionalProperties", false);
            ObjectNode properties = schema.putObject("properties");
            properties.putObject("command").put("type", "string");
            properties.putObject("message").put("type", "string").put("maxLength", 250);
            ArrayNode required = schema.putArray("required");
            required.add("command");
            required.add("message");
            return;
        }
        responseFormat.put("type", "json_object");
    }

    private HttpResponse<String> sendRequest(HttpRequest request) throws IOException {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new LLMServiceException(providerTarget.providerName + " request failed with HTTP status " + status);
            }
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    private String[] authHeaders() {
        if (apiKey == null || apiKey.isBlank()) {
            return new String[0];
        }
        return new String[]{"Authorization", "Bearer " + apiKey.trim()};
    }

    private static String extractContent(JsonNode root) {
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (contentNode.isTextual()) {
            return contentNode.asText("");
        }
        if (contentNode.isArray()) {
            for (JsonNode part : contentNode) {
                if ("text".equals(part.path("type").asText())) {
                    String text = part.path("text").asText("");
                    if (!text.isBlank()) {
                        return text;
                    }
                }
            }
        }
        return "";
    }

    private static String normalizeBaseUrl(String baseUrl, String fallback) {
        String normalized = (baseUrl == null || baseUrl.isBlank()) ? fallback : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.endsWith("/v1") ? normalized : normalized + "/v1";
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

    private static boolean shouldForceCommandJson(List<Message> messages) {
        for (Message message : messages) {
            if (message == null || message.getMessage() == null) {
                continue;
            }
            String role = message.getRole() == null ? "" : message.getRole().trim().toLowerCase();
            if (!"system".equals(role)) {
                continue;
            }
            String content = message.getMessage();
            boolean containsOutputShape = content.contains("\"command\"") && content.contains("\"message\"");
            boolean containsCommandInstructions = content.contains("VALID COMMANDS")
                    || content.contains("Respond ONLY with a single valid JSON object")
                    || content.contains("FINAL REMINDER: Output ONLY the JSON object");
            if (containsOutputShape && containsCommandInstructions) {
                return true;
            }
        }
        return false;
    }
}
