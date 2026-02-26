package me.sailex.secondbrain.client.gui.screen;

import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.DiscreteSliderComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextAreaComponent;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import me.sailex.secondbrain.client.networking.ClientNetworkManager;
import me.sailex.secondbrain.config.BaseConfig;
import me.sailex.secondbrain.networking.packet.UpdateBaseConfigPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static me.sailex.secondbrain.SecondBrain.MOD_ID;

public class BaseConfigScreen extends ConfigScreen<BaseConfig> {

    private static final Identifier ID = Identifier.of(MOD_ID, "baseconfig");
    private static final String LLM_TIMEOUT_LABEL = "LLM Service Timeout";
    private static final String CONTEXT_CHUNK_RADIUS_LABEL = "Chunk Radius";
    private static final String CONTEXT_VERTICAL_RANGE_LABEL = "Vertical Scan Range";
    private static final String CHUNK_EXPIRY_TIME_LABEL = "Chunk Expiry Time";
    private static final String VERBOSE_LABEL = "Debug Mode";
    private static final String OLLAMA_URL_LABEL = "Ollama URL";
    private static final String OPENAI_BASE_URL_LABEL = "OpenAI Compatible URL";
    private static final String OPENAI_API_KEY_LABEL = "OpenAI API Key";

    public BaseConfigScreen(
        ClientNetworkManager networkManager,
        BaseConfig baseConfig,
        boolean isEdit
    ) {
        super(networkManager, baseConfig, isEdit, ID);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        FlowLayout panel = rootComponent.childById(FlowLayout.class, "panel");

        panel.childById(LabelComponent.class, "llmTimeout-label").text(Text.of(LLM_TIMEOUT_LABEL));
        panel.childById(DiscreteSliderComponent.class, "llmTimeout")
                .setFromDiscreteValue(config.getLlmTimeout())
                .onChanged()
                .subscribe(value -> config.setLlmTimeout((int) Math.round(value)));

        panel.childById(LabelComponent.class, "chunkRadius-label").text(Text.of(CONTEXT_CHUNK_RADIUS_LABEL));
        panel.childById(DiscreteSliderComponent.class, "chunkRadius")
                .setFromDiscreteValue(config.getContextChunkRadius())
                .onChanged()
                .subscribe(value -> config.setContextChunkRadius((int) Math.round(value)));

        panel.childById(LabelComponent.class, "verticalScanRange-label").text(Text.of(CONTEXT_VERTICAL_RANGE_LABEL));
        panel.childById(DiscreteSliderComponent.class, "verticalScanRange")
                .setFromDiscreteValue(config.getContextVerticalScanRange())
                .onChanged()
                .subscribe(value -> config.setContextVerticalScanRange((int) Math.round(value)));

        panel.childById(LabelComponent.class, "cacheExpiryTime-label").text(Text.of(CHUNK_EXPIRY_TIME_LABEL));
        panel.childById(DiscreteSliderComponent.class, "cacheExpiryTime")
                .setFromDiscreteValue(config.getChunkExpiryTime())
                .onChanged()
                .subscribe(value -> config.setChunkExpiryTime((int) Math.round(value)));

        panel.childById(LabelComponent.class, "verbose-label").text(Text.of(VERBOSE_LABEL));
        panel.childById(CheckboxComponent.class, "verbose")
                .checked(config.isVerbose())
                .onChanged(config::setVerbose);

        panel.childById(LabelComponent.class, "ollamaUrl-label").text(Text.of(OLLAMA_URL_LABEL));
        panel.childById(TextAreaComponent.class, "ollamaUrl")
                .text(config.getOllamaUrl())
                .onChanged()
                .subscribe(config::setOllamaUrl);

        panel.childById(LabelComponent.class, "openaiBaseUrl-label").text(Text.of(OPENAI_BASE_URL_LABEL));
        panel.childById(TextAreaComponent.class, "openaiBaseUrl")
                .text(config.getOpenaiBaseUrl())
                .onChanged()
                .subscribe(config::setOpenaiBaseUrl);

        panel.childById(LabelComponent.class, "openaiApiKey-label").text(Text.of(OPENAI_API_KEY_LABEL));
        panel.childById(TextAreaComponent.class, "openaiApiKey")
                .text(config.getOpenaiApiKey())
                .onChanged()
                .subscribe(config::setOpenaiApiKey);

        onPressSaveButton(panel, button -> {
            networkManager.sendPacket(new UpdateBaseConfigPacket(config));
            close();
        });

        panel.childById(ButtonComponent.class, "cancel").onPress(button -> close());
    }
}
