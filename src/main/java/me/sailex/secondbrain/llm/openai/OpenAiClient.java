package me.sailex.secondbrain.llm.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.sailex.secondbrain.exception.LLMServiceException;
import me.sailex.secondbrain.history.Message;
import me.sailex.secondbrain.llm.LLMClient;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OpenAiClient implements LLMClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String OPENAI_TTS_MODEL = "gpt-4o-mini-tts";

    private final OpenAiCompatibleChatClient chatClient;
    private final HttpClient httpClient;
    private final String normalizedBaseUrl;
    private final String apiKey;
    private final Duration requestTimeout;
    private final String voiceId;
    private final List<SourceDataLine> activeLines = Collections.synchronizedList(new ArrayList<>());

	/**
	 * Constructor for OpenAiClient.
	 *
	 * @param apiKey  the api key
	 */
	public OpenAiClient(String model, String apiKey, String baseUrl, int timeout, String voiceId) {
        this.chatClient = new OpenAiCompatibleChatClient(
                model,
                apiKey,
                baseUrl,
                timeout,
                OpenAiCompatibleChatClient.ProviderTarget.OPENAI
        );
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(timeout, 1)))
                .build();
        this.normalizedBaseUrl = chatClient.getNormalizedBaseUrl();
        this.requestTimeout = Duration.ofSeconds(Math.max(timeout, 1));
        this.apiKey = apiKey;
        this.voiceId = voiceId;
	}

	@Override
	public Message chat(List<Message> messages) {
        return chatClient.chat(messages);
    }

    @Override
    public void checkServiceIsReachable() {
        chatClient.checkServiceIsReachable();
    }

    @Override
    public void stopService() {
        synchronized (activeLines) {
            for (SourceDataLine line : new ArrayList<>(activeLines)) {
                try {
                    line.stop();
                    line.flush();
                    line.close();
                } catch (Exception ignored) {
                    // best effort audio line cleanup
                }
            }
            activeLines.clear();
        }
	}

    public void startTextToSpeech(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        try {
            ObjectNode requestBody = MAPPER.createObjectNode();
            requestBody.put("model", OPENAI_TTS_MODEL);
            requestBody.put("voice", resolveVoice(voiceId));
            requestBody.put("input", message);
            requestBody.put("response_format", "wav");

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(normalizedBaseUrl + "/audio/speech"))
                    .timeout(requestTimeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(requestBody), StandardCharsets.UTF_8));
            if (apiKey != null && !apiKey.isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + apiKey.trim());
            }

            HttpResponse<byte[]> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new LLMServiceException("OpenAI TTS request failed with HTTP status " + response.statusCode());
            }

            playWav(response.body());
        } catch (Exception e) {
            Throwable root = e;
            while (root.getCause() != null) root = root.getCause();
            String safeRootMessage = "TTS provider returned an error.";
            throw new LLMServiceException("Failed to generate OpenAI TTS audio"
                    + "\nRoot cause: " + root.getClass().getSimpleName() + ": " + safeRootMessage, e);
        }
    }

    private static String resolveVoice(String selectedVoiceId) {
        if (selectedVoiceId == null) {
            return "alloy";
        }
        return switch (selectedVoiceId.trim().toLowerCase()) {
            case "ash" -> "ash";
            case "ballad" -> "ballad";
            case "coral" -> "coral";
            case "echo" -> "echo";
            case "sage" -> "sage";
            case "shimmer" -> "shimmer";
            case "verse" -> "verse";
            case "marin" -> "marin";
            case "cedar" -> "cedar";
            default -> "alloy";
        };
    }

    private void playWav(byte[] audioBytes) throws Exception {
        if (audioBytes == null || audioBytes.length == 0) {
            throw new LLMServiceException("OpenAI TTS returned empty audio");
        }
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(audioBytes);
             AudioInputStream sourceStream = AudioSystem.getAudioInputStream(byteArrayInputStream)) {
            AudioFormat sourceFormat = sourceStream.getFormat();
            AudioFormat targetFormat = sourceFormat;
            AudioInputStream playbackStream = sourceStream;

            boolean requiresPcm16 = !AudioFormat.Encoding.PCM_SIGNED.equals(sourceFormat.getEncoding())
                    || sourceFormat.getSampleSizeInBits() != 16;
            if (requiresPcm16) {
                AudioFormat pcm16 = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        sourceFormat.getSampleRate(),
                        16,
                        sourceFormat.getChannels(),
                        sourceFormat.getChannels() * 2,
                        sourceFormat.getSampleRate(),
                        false
                );
                if (AudioSystem.isConversionSupported(pcm16, sourceFormat)) {
                    targetFormat = pcm16;
                    playbackStream = AudioSystem.getAudioInputStream(pcm16, sourceStream);
                }
            }

            try (AudioInputStream playableStream = playbackStream) {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, targetFormat);
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                activeLines.add(line);
                try {
                    line.open(targetFormat);
                    line.start();
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = playableStream.read(buffer, 0, buffer.length)) != -1) {
                        if (bytesRead > 0) {
                            line.write(buffer, 0, bytesRead);
                        }
                    }
                    line.drain();
                } finally {
                    line.stop();
                    line.close();
                    activeLines.remove(line);
                }
            }
        }
    }

}
