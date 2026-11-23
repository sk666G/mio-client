package dev.schizo.client.feature;

import dev.schizo.client.setting.BooleanSetting;
import dev.schizo.client.setting.ModeSetting;

import java.util.List;

/** Displays the current movement keys on the game HUD. */
public final class KeystrokesFeature extends Feature {
    private final BooleanSetting showJump = addSetting(
        new BooleanSetting("show-jump", "Show jump key", true)
    );
    private final ModeSetting position = addSetting(
        new ModeSetting("position", "Position", List.of("Left", "Right"), "Right")
    );

    public KeystrokesFeature() {
        super("keystrokes", "Keystrokes", "Shows live WASD and jump key states.", FeatureCategory.RENDER);
    }

    public BooleanSetting showJump() { return showJump; }
    public ModeSetting position() { return position; }
}
