package dev.schizo.client.feature;

import dev.schizo.client.setting.BooleanSetting;

/** Displays local information about the living entity under the crosshair. */
public final class TargetHudFeature extends Feature {
    private final BooleanSetting showDistance = addSetting(
        new BooleanSetting("show-distance", "Show distance", true)
    );

    public TargetHudFeature() {
        super("target-hud", "Target HUD",
            "Displays the name, health and optional distance of the living entity under your crosshair.",
            FeatureCategory.RENDER);
    }

    public BooleanSetting showDistance() {
        return showDistance;
    }
}
