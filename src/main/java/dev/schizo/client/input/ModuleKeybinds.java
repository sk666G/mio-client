package dev.schizo.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import dev.schizo.client.FeatureRegistry;
import dev.schizo.client.SchizoClient;
import dev.schizo.client.config.ClientConfig;
import dev.schizo.client.feature.Feature;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

/** Registers one rebindable Controls entry for every feature module. */
public final class ModuleKeybinds {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath(SchizoClient.MOD_ID, "modules")
    );
    private static final Map<Feature, KeyMapping> BINDINGS = new LinkedHashMap<>();

    private ModuleKeybinds() {}

    public static void register() {
        for (Feature feature : FeatureRegistry.all()) {
            String translationKey = "key.schizo.module." + feature.id().replace('-', '_');
            KeyMapping binding = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                translationKey,
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
            ));
            BINDINGS.put(feature, binding);
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            for (Map.Entry<Feature, KeyMapping> entry : BINDINGS.entrySet()) {
                while (entry.getValue().consumeClick()) {
                    if (client.screen == null) {
                        entry.getKey().toggle();
                        ClientConfig.save();
                    }
                }
            }
        });
    }
}
