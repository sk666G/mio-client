package dev.schizo.client.feature;

import dev.schizo.client.setting.BooleanSetting;
import dev.schizo.client.setting.ModeSetting;

import java.util.List;

/** Shows the player's equipped armor and remaining durability. */
public final class ArmorHudFeature extends Feature {
    private final BooleanSetting showDurability = addSetting(
        new BooleanSetting("show-durability", "Show durability", true)
    );
    private final BooleanSetting showEmptySlots = addSetting(
        new BooleanSetting("show-empty", "Show empty slots", false)
    );
    private final ModeSetting position = addSetting(
        new ModeSetting("position", "Position", List.of("Left", "Right"), "Left")
    );

    public ArmorHudFeature() {
        super("armor-hud", "Armor HUD", "Shows equipped armor and remaining durability.", FeatureCategory.RENDER);
    }

    public BooleanSetting showDurability() { return showDurability; }
    public BooleanSetting showEmptySlots() { return showEmptySlots; }
    public ModeSetting position() { return position; }
}
