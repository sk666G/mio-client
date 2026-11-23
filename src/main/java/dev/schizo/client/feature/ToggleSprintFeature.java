package dev.schizo.client.feature;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

/**
 * Holds the vanilla sprint key while forward movement is requested.
 * This uses normal client input and leaves sprint eligibility to Minecraft.
 */
public final class ToggleSprintFeature extends Feature {
    public ToggleSprintFeature() {
        super("toggle-sprint", "Toggle Sprint", "Automatically holds sprint while moving forward.", FeatureCategory.MOVEMENT);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!isEnabled()) return;
            if (client.player == null || client.screen != null) {
                client.options.keySprint.setDown(false);
                return;
            }

            boolean wantsForward = client.options.keyUp.isDown();
            boolean wantsSneak = client.options.keyShift.isDown();
            client.options.keySprint.setDown(wantsForward && !wantsSneak);
        });
    }

    @Override
    protected void onDisable() {
        Minecraft.getInstance().options.keySprint.setDown(false);
    }
}
