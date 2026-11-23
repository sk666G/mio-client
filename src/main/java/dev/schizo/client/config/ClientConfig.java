package dev.schizo.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.schizo.client.FeatureRegistry;
import dev.schizo.client.SchizoClient;
import dev.schizo.client.feature.Feature;
import dev.schizo.client.setting.BooleanSetting;
import dev.schizo.client.setting.ModeSetting;
import dev.schizo.client.setting.NumberSetting;
import dev.schizo.client.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Reads and writes module state and settings to the user's config directory. */
public final class ClientConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("schizo.json");

    private ClientConfig() {}

    public static void load() {
        if (!Files.exists(FILE)) return;

        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(FILE));
            if (!parsed.isJsonObject()) throw new IllegalStateException("Config root must be an object");

            JsonObject modules = parsed.getAsJsonObject().getAsJsonObject("modules");
            if (modules == null) return;

            for (Feature feature : FeatureRegistry.all()) {
                JsonElement moduleElement = modules.get(feature.id());
                if (moduleElement == null || !moduleElement.isJsonObject()) continue;

                JsonObject module = moduleElement.getAsJsonObject();
                JsonElement enabled = module.get("enabled");
                if (enabled != null && enabled.isJsonPrimitive()
                        && enabled.getAsJsonPrimitive().isBoolean()) {
                    try {
                        feature.setEnabled(enabled.getAsBoolean());
                    } catch (RuntimeException exception) {
                        SchizoClient.LOGGER.warn("Could not restore module '{}'", feature.id(), exception);
                    }
                }

                JsonObject settings = module.getAsJsonObject("settings");
                if (settings == null) continue;
                for (Setting<?> setting : feature.settings()) {
                    JsonElement value = settings.get(setting.id());
                    if (value == null || !value.isJsonPrimitive()) continue;
                    try {
                        if (setting instanceof BooleanSetting bool && value.getAsJsonPrimitive().isBoolean()) {
                            bool.set(value.getAsBoolean());
                        } else if (setting instanceof NumberSetting number && value.getAsJsonPrimitive().isNumber()) {
                            number.set(value.getAsDouble());
                        } else if (setting instanceof ModeSetting mode && value.getAsJsonPrimitive().isString()) {
                            mode.set(value.getAsString());
                        }
                    } catch (RuntimeException exception) {
                        SchizoClient.LOGGER.warn("Ignoring invalid setting '{}.{}'", feature.id(), setting.id());
                    }
                }
            }
            SchizoClient.LOGGER.info("Loaded Schizo configuration from {}", FILE);
        } catch (Exception exception) {
            SchizoClient.LOGGER.error("Could not load Schizo configuration from {}", FILE, exception);
        }
    }

    public static void save() {
        JsonObject root = new JsonObject();
        JsonObject modules = new JsonObject();

        for (Feature feature : FeatureRegistry.all()) {
            JsonObject module = new JsonObject();
            module.addProperty("enabled", feature.isEnabled());

            JsonObject settings = new JsonObject();
            for (Setting<?> setting : feature.settings()) {
                if (setting instanceof BooleanSetting bool) {
                    settings.addProperty(setting.id(), bool.get());
                } else if (setting instanceof NumberSetting number) {
                    settings.addProperty(setting.id(), number.get());
                } else if (setting instanceof ModeSetting mode) {
                    settings.addProperty(setting.id(), mode.get());
                }
            }
            module.add("settings", settings);
            modules.add(feature.id(), module);
        }
        root.add("modules", modules);

        Path temporary = FILE.resolveSibling(FILE.getFileName() + ".tmp");
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(temporary, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root));
            try {
                Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveFailure) {
                Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            SchizoClient.LOGGER.error("Could not save Schizo configuration to {}", FILE, exception);
        }
    }
}
