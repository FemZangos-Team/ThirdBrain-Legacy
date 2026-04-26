package me.sailex.secondbrain.llm.ollama;

import me.sailex.secondbrain.history.Message;
import me.sailex.secondbrain.llm.LLMClient;
import me.sailex.secondbrain.llm.openai.OpenAiCompatibleChatClient;

import java.util.List;

public class OllamaClient implements LLMClient {

    private final OpenAiCompatibleChatClient chatClient;

    public OllamaClient(String model, String url, int timeout, boolean verbose) {
        this.chatClient = new OpenAiCompatibleChatClient(
                model,
                "",
                url,
                timeout,
                OpenAiCompatibleChatClient.ProviderTarget.OLLAMA
        );
    }

    @Override
    public void checkServiceIsReachable() {
        chatClient.checkServiceIsReachable();
    }

    @Override
    public Message chat(List<Message> messages) {
        return chatClient.chat(messages);
    }

    @Override
    public void stopService() {
        // nothing to stop
    }
}
