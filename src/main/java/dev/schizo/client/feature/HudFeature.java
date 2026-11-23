package dev.schizo.client.feature;

import dev.schizo.client.setting.BooleanSetting;

/** Configurable informational HUD overlay. */
public final class HudFeature extends Feature {
    private final BooleanSetting showCoordinates = addSetting(
        new BooleanSetting("coordinates", "Show coordinates", true)
    );
    private final BooleanSetting showDirection = addSetting(
        new BooleanSetting("direction", "Show facing direction", true)
    );
    private final BooleanSetting showModules = addSetting(
        new BooleanSetting("enabled-modules", "Show enabled modules", true)
    );

    public HudFeature() {
        super("information-hud", "Information HUD", "Displays local position, facing direction and enabled modules.", FeatureCategory.RENDER);
    }

    public BooleanSetting showCoordinates() { return showCoordinates; }
    public BooleanSetting showDirection() { return showDirection; }
    public BooleanSetting showModules() { return showModules; }
}
