package dev.schizo.client.feature;

import dev.schizo.client.setting.BooleanSetting;
import dev.schizo.client.setting.NumberSetting;

/** Shows active potion and status effects with their remaining time. */
public final class EffectsHudFeature extends Feature {
    private final BooleanSetting showDuration = addSetting(
        new BooleanSetting("show-duration", "Show duration", true)
    );
    private final BooleanSetting showAmplifier = addSetting(
        new BooleanSetting("show-amplifier", "Show amplifier", true)
    );
    private final NumberSetting maxRows = addSetting(
        new NumberSetting("max-rows", "Maximum effects", 6, 1, 12, 1)
    );

    public EffectsHudFeature() {
        super("effects-hud", "Effects HUD", "Lists active status effects and remaining duration.", FeatureCategory.RENDER);
    }

    public BooleanSetting showDuration() { return showDuration; }
    public BooleanSetting showAmplifier() { return showAmplifier; }
    public NumberSetting maxRows() { return maxRows; }
}
