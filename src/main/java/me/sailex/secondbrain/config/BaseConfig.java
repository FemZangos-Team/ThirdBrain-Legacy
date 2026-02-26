package me.sailex.secondbrain.config;

import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class BaseConfig implements Configurable {
    private int llmTimeout = 10;
    private int contextChunkRadius = 4;
    private int contextVerticalScanRange = 8;
    private int chunkExpiryTime = 60;
    private boolean verbose = false;
    private String ollamaUrl = "http://localhost:11434";
    private String openaiBaseUrl = "https://api.openai.com/v1";
    private String openaiApiKey = "";

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
        return ollamaUrl;
    }

    public void setOllamaUrl(String ollamaUrl) {
        this.ollamaUrl = ollamaUrl;
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
                ",openaiApiKey=***}";
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
        return copied;
    }

}
