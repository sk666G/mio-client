package dev.schizo.client.feature;

import dev.schizo.client.setting.BooleanSetting;

/** Displays the number of totems carried in the player's inventory and offhand. */
public final class TotemCounterFeature extends Feature {
    private final BooleanSetting showZero = addSetting(
        new BooleanSetting("show-zero", "Show when count is zero", false)
    );

    public TotemCounterFeature() {
        super("totem-counter", "Totem Counter",
            "Shows the number of Totems of Undying in your inventory and offhand.",
            FeatureCategory.RENDER);
    }

    public BooleanSetting showZero() {
        return showZero;
    }
}
