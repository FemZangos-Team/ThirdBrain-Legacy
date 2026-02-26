package me.sailex.secondbrain.client.gui.screen;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextAreaComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import me.sailex.secondbrain.client.networking.ClientNetworkManager;
import me.sailex.secondbrain.config.BaseConfig;
import me.sailex.secondbrain.config.NPCConfig;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.IntConsumer;

/*? >=1.21.11 {*/
import static io.wispforest.owo.ui.component.UIComponents.*;
import static io.wispforest.owo.ui.container.UIContainers.*;
/*?} else {*/
import static io.wispforest.owo.ui.component.Components.*;
import static io.wispforest.owo.ui.container.Containers.*;
/*?}*/
import static me.sailex.secondbrain.SecondBrain.MOD_ID;

public class NPCZoneBehaviorScreen extends ConfigScreen<NPCConfig> {

    private static final Identifier ID = Identifier.of(MOD_ID, "npczones");
    private static final int WIDE_INPUT_WIDTH = 42;
    private static final int SINGLE_LINE_INPUT_HEIGHT = 9;
    private static final int ZONE_INSTRUCTIONS_HEIGHT = 22;
    private static final int INTEGER_INPUT_WIDTH = 38;

    private final List<NPCConfig> existingConfigs;
    private final boolean returnToMainScreen;
    private final BaseConfig baseConfig;
    private final IdentityHashMap<NPCConfig.ZoneBehavior, Boolean> expandedZones = new IdentityHashMap<>();

    public NPCZoneBehaviorScreen(
            ClientNetworkManager networkManager,
            NPCConfig npcConfig,
            boolean isEdit,
            List<NPCConfig> existingConfigs
    ) {
        super(networkManager, npcConfig, isEdit, ID);
        this.existingConfigs = existingConfigs;
        this.returnToMainScreen = false;
        this.baseConfig = null;
    }

    public NPCZoneBehaviorScreen(
            ClientNetworkManager networkManager,
            NPCConfig npcConfig,
            List<NPCConfig> existingConfigs,
            BaseConfig baseConfig
    ) {
        super(networkManager, npcConfig, true, ID);
        this.existingConfigs = existingConfigs;
        this.returnToMainScreen = true;
        this.baseConfig = baseConfig;
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        FlowLayout panel = rootComponent.childById(FlowLayout.class, "panel");
        panel.childById(LabelComponent.class, "title")
                .text(Text.of("Zones for '" + config.getNpcName() + "'"));

        FlowLayout zoneList = panel.childById(FlowLayout.class, "zoneList");
        renderZones(zoneList);

        rootComponent.childById(ButtonComponent.class, "add_zone").onPress(button -> {
            int nextZoneNumber = config.getZoneBehaviors().size() + 1;
            NPCConfig.ZoneBehavior newZone = new NPCConfig.ZoneBehavior(
                    "Zone " + nextZoneNumber,
                    new NPCConfig.ZoneCoordinate(0, 0, 0),
                    new NPCConfig.ZoneCoordinate(0, 0, 0),
                    0,
                    ""
            );
            config.getZoneBehaviors().add(newZone);
            expandedZones.put(newZone, true);
            renderZones(zoneList);
        });

        onPressSaveButton(rootComponent, button -> goBackToNpcConfig());
        rootComponent.childById(ButtonComponent.class, "cancel").onPress(button -> goBackToNpcConfig());
    }

    private void goBackToNpcConfig() {
        if (returnToMainScreen && baseConfig != null) {
            client.setScreen(new SecondBrainScreen(existingConfigs, baseConfig, networkManager));
            return;
        }
        client.setScreen(new NPCConfigScreen(networkManager, config, isEdit, existingConfigs, this));
    }

    private void renderZones(FlowLayout zoneList) {
        zoneList.clearChildren();
        expandedZones.keySet().removeIf(zone -> !config.getZoneBehaviors().contains(zone));
        if (config.getZoneBehaviors().isEmpty()) {
            zoneList.child(label(Text.of("No zones configured yet.")).shadow(true));
            return;
        }

        for (int i = 0; i < config.getZoneBehaviors().size(); i++) {
            int zoneIndex = i;
            NPCConfig.ZoneBehavior zone = config.getZoneBehaviors().get(i);

            FlowLayout zoneCard = verticalFlow(Sizing.fill(96), Sizing.content());
            zoneCard.gap(4);
            zoneCard.padding(Insets.of(6));
            zoneCard.surface(Surface.DARK_PANEL);
            boolean isExpanded = expandedZones.getOrDefault(zone, true);
            zoneCard.child(button(
                    Text.of((isExpanded ? "[-] " : "[+] ") + "Zone " + (i + 1)),
                    button -> {
                        expandedZones.put(zone, !expandedZones.getOrDefault(zone, true));
                        renderZones(zoneList);
                    }
            ).sizing(Sizing.fill(100), Sizing.content()));

            if (isExpanded) {
                FlowLayout zoneDetails = verticalFlow(Sizing.fill(100), Sizing.content());
                zoneDetails.gap(4);

                zoneDetails.child(label(Text.of("Name")).shadow(true));
                TextAreaComponent zoneNameInput = textArea(
                        Sizing.fill(WIDE_INPUT_WIDTH),
                        Sizing.fill(SINGLE_LINE_INPUT_HEIGHT)
                ).text(zone.getName());
                zoneNameInput.onChanged().subscribe(zone::setName);
                zoneDetails.child(zoneNameInput);

                zoneDetails.child(label(Text.of("Priority")).shadow(true));
                zoneDetails.child(createIntegerInput(zone.getPriority(), zone::setPriority));

                zoneDetails.child(label(Text.of("From (x, y, z)")).shadow(true));
                zoneDetails.child(createCoordinateRow(zone.getFrom()));

                zoneDetails.child(label(Text.of("To (x, y, z)")).shadow(true));
                zoneDetails.child(createCoordinateRow(zone.getTo()));

                zoneDetails.child(label(Text.of("Extra Instructions")).shadow(true));
                TextAreaComponent instructionsInput = textArea(
                        Sizing.fill(WIDE_INPUT_WIDTH),
                        Sizing.fill(ZONE_INSTRUCTIONS_HEIGHT)
                ).text(zone.getInstructions());
                instructionsInput.onChanged().subscribe(zone::setInstructions);
                zoneDetails.child(instructionsInput);

                zoneDetails.child(button(Text.of("Remove Zone"), button -> {
                    expandedZones.remove(zone);
                    config.getZoneBehaviors().remove(zoneIndex);
                    renderZones(zoneList);
                }));
                zoneCard.child(zoneDetails);
            }

            zoneList.child(zoneCard);
        }
    }

    private FlowLayout createCoordinateRow(NPCConfig.ZoneCoordinate coordinate) {
        FlowLayout row = horizontalFlow(Sizing.content(), Sizing.content());
        row.gap(4);
        row.child(label(Text.of("x")).shadow(true));
        row.child(createIntegerInput(coordinate.getX(), coordinate::setX));
        row.child(label(Text.of("y")).shadow(true));
        row.child(createIntegerInput(coordinate.getY(), coordinate::setY));
        row.child(label(Text.of("z")).shadow(true));
        row.child(createIntegerInput(coordinate.getZ(), coordinate::setZ));
        return row;
    }

    private TextAreaComponent createIntegerInput(int currentValue, IntConsumer setter) {
        TextAreaComponent input = textArea(
                Sizing.fixed(INTEGER_INPUT_WIDTH),
                Sizing.fill(SINGLE_LINE_INPUT_HEIGHT)
        ).text(String.valueOf(currentValue));
        input.onChanged().subscribe(value -> {
            Integer parsed = parseInteger(value);
            if (parsed != null) {
                setter.accept(parsed);
            }
        });
        return input;
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
