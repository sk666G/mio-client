package dev.schizo.client;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Central registry for client-side features. */
public final class FeatureRegistry {
    private static final Map<String, Feature> FEATURES = new LinkedHashMap<>();

    private FeatureRegistry() {}

    public static void registerDefaults() {
        register(new Feature("example", "Example", "Disabled example feature."));
        SchizoClient.LOGGER.info("Registered {} Schizo features", FEATURES.size());
    }

    public static void register(Feature feature) {
        if (FEATURES.putIfAbsent(feature.id(), feature) != null) {
            throw new IllegalStateException("Duplicate feature id: " + feature.id());
        }
    }

    public static Map<String, Feature> all() {
        return Collections.unmodifiableMap(FEATURES);
    }

    public record Feature(String id, String name, String description) {
        public Feature {
            if (id == null || id.isBlank() || name == null || name.isBlank()) {
                throw new IllegalArgumentException("Feature id and name are required");
            }
        }
    }
}
