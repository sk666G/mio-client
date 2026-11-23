package dev.schizo.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client entrypoint for Schizo. */
public final class SchizoClient implements ClientModInitializer {
    public static final String MOD_ID = "schizo";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Schizo client initialized");
        FeatureRegistry.registerDefaults();
    }
}
