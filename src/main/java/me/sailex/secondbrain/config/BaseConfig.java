package me.sailex.secondbrain.config;

import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class BaseConfig implements Configurable {
    private static final String DEFAULT_OLLAMA_URL = "http://localhost:11434/v1";

    private int llmTimeout = 10;
    private int contextChunkRadius = 4;
    private int contextVerticalScanRange = 8;
    private int chunkExpiryTime = 60;
    private boolean verbose = false;
    private String ollamaUrl = DEFAULT_OLLAMA_URL;
    private String openaiBaseUrl = "https://api.openai.com/v1";
    private String openaiApiKey = "";
    private String openwebuiBaseUrl = "http://localhost:3000";
    private String openwebuiApiKey = "";

    public int getLlmTimeout() {
        return llmTimeout;
    }

    public int getContextVerticalScanRange() {
        return contextVerticalScanRange;
    }

    public int getContextChunkRadius() {
        return contextChunkRadius;
    }

    public int getChunkExpiryTime() {
        return chunkExpiryTime;
    }

    public void setContextChunkRadius(int contextChunkRadius) {
        this.contextChunkRadius = contextChunkRadius;
    }

    public void setChunkExpiryTime(int chunkExpiryTime) {
        this.chunkExpiryTime = chunkExpiryTime;
    }

    public void setContextVerticalScanRange(int contextVerticalScanRange) {
        this.contextVerticalScanRange = contextVerticalScanRange;
    }

    public void setLlmTimeout(int llmTimeout) {
        this.llmTimeout = llmTimeout;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public String getOllamaUrl() {
        return normalizeOllamaUrl(ollamaUrl);
    }

    public void setOllamaUrl(String ollamaUrl) {
        this.ollamaUrl = normalizeOllamaUrl(ollamaUrl);
    }

    public String getOpenaiBaseUrl() {
        return openaiBaseUrl;
    }

    public void setOpenaiBaseUrl(String openaiBaseUrl) {
        this.openaiBaseUrl = openaiBaseUrl;
    }

    public String getOpenaiApiKey() {
        return openaiApiKey;
    }

    public void setOpenaiApiKey(String openaiApiKey) {
        this.openaiApiKey = openaiApiKey;
    }

    public String getOpenwebuiBaseUrl() {
        return openwebuiBaseUrl;
    }

    public void setOpenwebuiBaseUrl(String openwebuiBaseUrl) {
        this.openwebuiBaseUrl = openwebuiBaseUrl;
    }

    public String getOpenwebuiApiKey() {
        return openwebuiApiKey;
    }

    public void setOpenwebuiApiKey(String openwebuiApiKey) {
        this.openwebuiApiKey = openwebuiApiKey;
    }

    @Override
    public String getConfigName() {
        return "base";
    }

    public static final StructEndec<BaseConfig> ENDEC = StructEndecBuilder.of(
            Endec.INT.fieldOf("llmTimeout", BaseConfig::getLlmTimeout),
            Endec.INT.fieldOf("contextChunkRadius", BaseConfig::getContextChunkRadius),
            Endec.INT.fieldOf("contextVerticalScanRange", BaseConfig::getContextVerticalScanRange),
            Endec.INT.fieldOf("chunkExpiryTime", BaseConfig::getChunkExpiryTime),
            Endec.BOOLEAN.fieldOf("verbose", BaseConfig::isVerbose),
            Endec.STRING.fieldOf("ollamaUrl", BaseConfig::getOllamaUrl),
            Endec.STRING.fieldOf("openaiBaseUrl", BaseConfig::getOpenaiBaseUrl),
            Endec.STRING.fieldOf("openaiApiKey", BaseConfig::getOpenaiApiKey),
            Endec.STRING.fieldOf("openwebuiBaseUrl", BaseConfig::getOpenwebuiBaseUrl),
            Endec.STRING.fieldOf("openwebuiApiKey", BaseConfig::getOpenwebuiApiKey),
            BaseConfig::new
    );

    @Override
    public String toString() {
        return "BaseConfig{" +
                "llmTimeout=" + llmTimeout +
                ",contextChunkRadius=" + contextChunkRadius +
                ",contextVerticalScanRange=" + contextVerticalScanRange +
                ",chunkExpiryTime=" + chunkExpiryTime +
                ",verbose=" + verbose +
                ",ollamaUrl=" + ollamaUrl +
                ",openaiBaseUrl=" + openaiBaseUrl +
                ",openaiApiKey=***" +
                ",openwebuiBaseUrl=" + openwebuiBaseUrl +
                ",openwebuiApiKey=***}";
    }

    public static BaseConfig deepCopy(BaseConfig config) {
        BaseConfig copied = new BaseConfig();
        copied.setLlmTimeout(config.getLlmTimeout());
        copied.setContextChunkRadius(config.getContextChunkRadius());
        copied.setContextVerticalScanRange(config.getContextVerticalScanRange());
        copied.setChunkExpiryTime(config.getChunkExpiryTime());
        copied.setVerbose(config.isVerbose());
        copied.setOllamaUrl(config.getOllamaUrl());
        copied.setOpenaiBaseUrl(config.getOpenaiBaseUrl());
        copied.setOpenaiApiKey(config.getOpenaiApiKey());
        copied.setOpenwebuiBaseUrl(config.getOpenwebuiBaseUrl());
        copied.setOpenwebuiApiKey(config.getOpenwebuiApiKey());
        return copied;
    }

    private static String normalizeOllamaUrl(String url) {
        if (url == null || url.isBlank()) {
            return DEFAULT_OLLAMA_URL;
        }
        String normalized = url.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.endsWith("/v1") ? normalized : normalized + "/v1";
    }

}
