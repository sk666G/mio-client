package dev.schizo.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.schizo.client.config.ClientConfig;
import dev.schizo.client.feature.ArmorHudFeature;
import dev.schizo.client.feature.ExampleFeature;
import dev.schizo.client.feature.HudFeature;
import dev.schizo.client.feature.ToggleSprintFeature;
import dev.schizo.client.gui.ClickGuiScreen;
import dev.schizo.client.hud.HudOverlay;
import dev.schizo.client.input.ModuleKeybinds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client entrypoint for Schizo. */
public final class SchizoClient implements ClientModInitializer {
    public static final String MOD_ID = "schizo";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath(MOD_ID, "main")
    );
    private static final KeyMapping OPEN_GUI = KeyBindingHelper.registerKeyBinding(
        new KeyMapping("key.schizo.open_gui", InputConstants.Type.KEYSYM,
            InputConstants.KEY_RSHIFT, KEY_CATEGORY)
    );

    @Override
    public void onInitializeClient() {
        FeatureRegistry.register(new ExampleFeature());
        FeatureRegistry.register(new HudFeature());
        FeatureRegistry.register(new ArmorHudFeature());
        FeatureRegistry.register(new ToggleSprintFeature());
        FeatureRegistry.registerDefaults();
        ModuleKeybinds.register();
        ClientConfig.load();
        HudOverlay.register();

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ClientConfig.save());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_GUI.consumeClick()) {
                if (client.screen instanceof ClickGuiScreen) {
                    client.setScreen(null);
                } else {
                    client.setScreen(new ClickGuiScreen(client.screen));
                }
            }
        });

        LOGGER.info("Schizo client initialized");
    }
}
