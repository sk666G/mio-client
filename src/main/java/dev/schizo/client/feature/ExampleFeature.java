package dev.schizo.client.feature;

import dev.schizo.client.setting.BooleanSetting;
import dev.schizo.client.setting.ModeSetting;
import dev.schizo.client.setting.NumberSetting;

import java.util.List;

/** Demonstration module used to exercise the GUI and setting controls. */
public final class ExampleFeature extends Feature {
    public ExampleFeature() {
        super("example", "Example", "Demonstrates module settings and GUI controls.", FeatureCategory.MISC);
        addSetting(new BooleanSetting("example-notice", "Show notice", true));
        addSetting(new NumberSetting("example-scale", "Scale", 1.0, 0.5, 2.0, 0.1));
        addSetting(new ModeSetting("example-style", "Style", List.of("Classic", "Compact", "Minimal"), "Classic"));
    }
}
