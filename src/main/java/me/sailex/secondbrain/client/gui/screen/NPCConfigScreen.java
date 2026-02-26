package me.sailex.secondbrain.client.gui.screen;

import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.FlowLayout;

import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import me.sailex.secondbrain.client.networking.ClientNetworkManager;
import me.sailex.secondbrain.config.NPCConfig;
import me.sailex.secondbrain.llm.LLMType;
import me.sailex.secondbrain.networking.packet.CreateNpcPacket;
import me.sailex.secondbrain.networking.packet.UpdateNpcConfigPacket;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.List;
import java.util.Optional;

/*? >=1.21.11 {*/
import static io.wispforest.owo.ui.component.UIComponents.*;
import static io.wispforest.owo.ui.container.UIContainers.*;
/*?} else {*/
import static io.wispforest.owo.ui.component.Components.*;
import static io.wispforest.owo.ui.container.Containers.*;
/*?}*/
import static me.sailex.secondbrain.SecondBrain.MOD_ID;

public class NPCConfigScreen extends ConfigScreen<NPCConfig> {

    private static final Identifier ID = Identifier.of(MOD_ID, "npcconfig");
    private static final int WIDE_INPUT_WIDTH = 42;
    private static final int HALF_INPUT_WIDTH = 24;
    private static final int SINGLE_LINE_INPUT_HEIGHT = 9;
    private static final int CHARACTER_INPUT_HEIGHT = 30;
    private static final int MIN_CONVERSATION_RANGE = 2;
    private static final int MAX_CONVERSATION_RANGE = 64;
    private static final List<SkinOption> SKIN_OPTIONS = List.of(
            new SkinOption("Random", ""),
            new SkinOption("Steve", "https://minecraft.wiki/Special:FilePath/Char.png"),
            new SkinOption("Alex", "https://minecraft.wiki/Special:FilePath/Alex%20skin.png"),
            new SkinOption("Ari", "https://minecraft.wiki/Special:FilePath/Ari%20(classic%20texture)%20JE1.png"),
            new SkinOption("Kai", "https://minecraft.wiki/Special:FilePath/Kai%20(classic%20texture)%20JE1.png"),
            new SkinOption("Noor", "https://minecraft.wiki/Special:FilePath/Noor%20(classic%20texture)%20JE1.png"),
            new SkinOption("Sunny", "https://minecraft.wiki/Special:FilePath/Sunny%20(classic%20texture)%20JE1.png"),
            new SkinOption("Zuri", "https://minecraft.wiki/Special:FilePath/Zuri%20(classic%20texture)%20JE1.png"),
            new SkinOption("Efe", "https://minecraft.wiki/Special:FilePath/Efe%20(classic%20texture)%20JE1.png"),
            new SkinOption("Makena", "https://minecraft.wiki/Special:FilePath/Makena%20(classic%20texture)%20JE1.png")
    );
    private static final List<VoiceOption> OPENAI_VOICE_OPTIONS = List.of(
            new VoiceOption("Alloy", "alloy"),
            new VoiceOption("Ash", "ash"),
            new VoiceOption("Ballad", "ballad"),
            new VoiceOption("Coral", "coral"),
            new VoiceOption("Echo", "echo"),
            new VoiceOption("Sage", "sage"),
            new VoiceOption("Shimmer", "shimmer"),
            new VoiceOption("Verse", "verse"),
            new VoiceOption("Marin", "marin"),
            new VoiceOption("Cedar", "cedar")
    );
    private final List<NPCConfig> existingConfigs;

    public NPCConfigScreen(
        ClientNetworkManager networkManager,
        NPCConfig npcConfig,
        boolean isEdit,
        List<NPCConfig> existingConfigs
    ) {
        super(networkManager, npcConfig, isEdit, ID);
        this.existingConfigs = existingConfigs;
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        FlowLayout panel = rootComponent.childById(FlowLayout.class, "panel");

        LabelComponent npcNameLabel = panel.childById(LabelComponent.class, "npcName-label");
        if (isEdit) {
            npcNameLabel.text(Text.of(NPCConfig.EDIT_NPC.formatted(config.getNpcName())));
        } else {
            applyLastUsedDefaults(config.getLlmType());
            npcNameLabel.text(Text.of(NPCConfig.NPC_NAME));
            TextAreaComponent npcName = textArea(Sizing.fill(WIDE_INPUT_WIDTH), Sizing.fill(SINGLE_LINE_INPUT_HEIGHT))
                    .text(config.getNpcName());
            npcName.onChanged().subscribe(config::setNpcName);
            panel.childById(FlowLayout.class, "npcName").child(npcName);
        }

        drawConversationRangeSection(panel);
        drawLLMTypeDropDown(panel);
        drawLLMModelInput(panel);

        //draw without any dropdown click the fields of active llmType
        drawLlmInfo(panel);

        onPressSaveButton(rootComponent, button -> {
            if (isEdit) {
                networkManager.sendPacket(new UpdateNpcConfigPacket(config));
                close();
            } else {
                networkManager.sendPacket(new CreateNpcPacket(config));
                close();
            }
        });

        rootComponent.childById(ButtonComponent.class, "cancel").onPress(button -> close());
    }

    private void drawLlmInfo(FlowLayout panel) {
        FlowLayout llmInfo = panel.childById(FlowLayout.class, "llmInfo");
        llmInfo.clearChildren();
        llmInfo.gap(4);

        switch (config.getLlmType()) {
            case OLLAMA -> {}
            case PLAYER2 -> {
                llmInfo.child(buildUseTtsCheckbox());
            }
            case OPENAI -> llmInfo.child(buildUseTtsCheckbox());
        }
        //system prompt
        llmInfo.child(label(Text.of(NPCConfig.LLM_CHARACTER)).shadow(true).margins(Insets.top(7)));
        TextAreaComponent llmCharacter = textArea(Sizing.fill(WIDE_INPUT_WIDTH), Sizing.fill(CHARACTER_INPUT_HEIGHT));
        llmCharacter.text(config.getLlmCharacter())
                .onChanged()
                .subscribe(config::setLlmCharacter);
        llmInfo.child(llmCharacter);

        drawSkinSelector(llmInfo);
        if (config.getLlmType() == LLMType.OPENAI) {
            drawOpenAiVoiceSelector(llmInfo);
        }
    }

    private void drawConversationRangeSection(FlowLayout panel) {
        panel.childById(LabelComponent.class, "conversationRange-label")
                .text(Text.of(NPCConfig.CONVERSATION_RANGE));

        DiscreteSliderComponent conversationRangeSlider = panel.childById(DiscreteSliderComponent.class, "conversationRangeSlider");
        conversationRangeSlider
                .setFromDiscreteValue(Math.max(MIN_CONVERSATION_RANGE,
                        Math.min(MAX_CONVERSATION_RANGE, config.getConversationRange())));

        conversationRangeSlider
                .onChanged()
                .subscribe(value -> config.setConversationRange((int) Math.round(value)));
    }

    private CheckboxComponent buildUseTtsCheckbox() {
        return checkbox(Text.of("Use TTS"))
                .checked(config.isTTS())
                .onChanged(config::setTTS);
    }

    private void drawLLMTypeDropDown(FlowLayout panel) {
        panel.childById(LabelComponent.class, "llmType-label").text(Text.of(NPCConfig.LLM_TYPE));
        DropdownComponent llmTypeDropDown = panel.childById(DropdownComponent.class, "llmType");
        ((FlowLayout) llmTypeDropDown.children().get(0)).clearChildren();
        if (isEdit) {
            llmTypeDropDown.button(
                    Text.of(config.getLlmType().toString()), button -> {});
        } else {
                llmTypeDropDown.button(
                    Text.of((config.getLlmType() == LLMType.OLLAMA ? "[X] " : "[ ] ") + LLMType.OLLAMA),
                    button -> {
                        config.setLlmType(LLMType.OLLAMA);
                        applyLastUsedDefaults(LLMType.OLLAMA);
                        drawLLMTypeDropDown(panel);
                        drawLLMModelInput(panel);
                        drawLlmInfo(panel);
                    });
            llmTypeDropDown.button(
                    Text.of((config.getLlmType() == LLMType.OPENAI ? "[X] " : "[ ] ") + LLMType.OPENAI),
                    button -> {
                        config.setLlmType(LLMType.OPENAI);
                        applyLastUsedDefaults(LLMType.OPENAI);
                        drawLLMTypeDropDown(panel);
                        drawLLMModelInput(panel);
                        drawLlmInfo(panel);
                    });
        }
    }

    private void drawLLMModelInput(FlowLayout panel) {
        FlowLayout llmModelContainer = panel.childById(FlowLayout.class, "llmModel");
        llmModelContainer.clearChildren();
        llmModelContainer.child(label(Text.of(NPCConfig.LLM_MODEL)).shadow(true));
        switch (config.getLlmType()) {
            case OLLAMA, OPENAI -> {
                TextAreaComponent llmModel = textArea(Sizing.fill(HALF_INPUT_WIDTH), Sizing.fill(SINGLE_LINE_INPUT_HEIGHT))
                        .text(config.getLlmModel());
                llmModel.onChanged().subscribe(config::setLlmModel);
                llmModelContainer.child(llmModel);
            }
        }
    }

    private void drawSkinSelector(FlowLayout llmInfo) {
        llmInfo.child(label(Text.of("NPC Skin")).shadow(true).margins(Insets.top(7)));
        final int[] selectedIndex = {getSkinOptionIndex(config.getSkinUrl())};
        LabelComponent selectedSkin = label(Text.of(SKIN_OPTIONS.get(selectedIndex[0]).name())).shadow(true);

        FlowLayout row = horizontalFlow(Sizing.fixed(150), Sizing.content());
        row.gap(4);
        selectedSkin.sizing(Sizing.fixed(100), Sizing.content());

        row.child(button(Text.of("<"), button -> {
            selectedIndex[0] = Math.floorMod(selectedIndex[0] - 1, SKIN_OPTIONS.size());
            applySkinSelection(selectedIndex[0], selectedSkin);
        }).sizing(Sizing.fixed(20), Sizing.content()));
        row.child(selectedSkin);
        row.child(button(Text.of(">"), button -> {
            selectedIndex[0] = Math.floorMod(selectedIndex[0] + 1, SKIN_OPTIONS.size());
            applySkinSelection(selectedIndex[0], selectedSkin);
        }).sizing(Sizing.fixed(20), Sizing.content()));

        llmInfo.child(row);
    }

    private void drawOpenAiVoiceSelector(FlowLayout llmInfo) {
        llmInfo.child(label(Text.of("NPC Voice")).shadow(true).margins(Insets.top(7)));
        final int[] selectedIndex = {getOpenAiVoiceIndex(config.getVoiceId())};
        LabelComponent selectedVoice = label(Text.of(OPENAI_VOICE_OPTIONS.get(selectedIndex[0]).name())).shadow(true);

        FlowLayout row = horizontalFlow(Sizing.fixed(150), Sizing.content());
        row.gap(4);
        selectedVoice.sizing(Sizing.fixed(100), Sizing.content());

        row.child(button(Text.of("<"), button -> {
            selectedIndex[0] = Math.floorMod(selectedIndex[0] - 1, OPENAI_VOICE_OPTIONS.size());
            applyVoiceSelection(selectedIndex[0], selectedVoice);
        }).sizing(Sizing.fixed(20), Sizing.content()));
        row.child(selectedVoice);
        row.child(button(Text.of(">"), button -> {
            selectedIndex[0] = Math.floorMod(selectedIndex[0] + 1, OPENAI_VOICE_OPTIONS.size());
            applyVoiceSelection(selectedIndex[0], selectedVoice);
        }).sizing(Sizing.fixed(20), Sizing.content()));

        llmInfo.child(row);
        applyVoiceSelection(selectedIndex[0], selectedVoice);
    }

    private void applySkinSelection(int index, LabelComponent selectedSkin) {
        SkinOption selected = SKIN_OPTIONS.get(index);
        selectedSkin.text(Text.of(selected.name()));
        config.setSkinUrl(selected.url());
    }

    private int getSkinOptionIndex(String currentUrl) {
        for (int i = 0; i < SKIN_OPTIONS.size(); i++) {
            if (SKIN_OPTIONS.get(i).url().equals(currentUrl)) {
                return i;
            }
        }
        return 0;
    }

    private void applyVoiceSelection(int index, LabelComponent selectedVoice) {
        VoiceOption selected = OPENAI_VOICE_OPTIONS.get(index);
        selectedVoice.text(Text.of(selected.name()));
        config.setVoiceId(selected.value());
    }

    private int getOpenAiVoiceIndex(String currentVoiceId) {
        if (currentVoiceId == null || currentVoiceId.isBlank()) {
            return 0;
        }
        for (int i = 0; i < OPENAI_VOICE_OPTIONS.size(); i++) {
            if (OPENAI_VOICE_OPTIONS.get(i).value().equalsIgnoreCase(currentVoiceId.trim())) {
                return i;
            }
        }
        return 0;
    }

    private void applyLastUsedDefaults(LLMType llmType) {
        Optional<NPCConfig> matching = findLastConfigByType(llmType);
        if (matching.isPresent()) {
            NPCConfig previous = matching.get();
            config.setLlmModel(previous.getLlmModel());
        }
    }

    private Optional<NPCConfig> findLastConfigByType(LLMType llmType) {
        for (int i = existingConfigs.size() - 1; i >= 0; i--) {
            NPCConfig candidate = existingConfigs.get(i);
            if (candidate.getLlmType() == llmType) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private record SkinOption(String name, String url) {}

    private record VoiceOption(String name, String value) {}
}
