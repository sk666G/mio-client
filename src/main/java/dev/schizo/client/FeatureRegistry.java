package dev.schizo.client;

import dev.schizo.client.feature.Feature;
import dev.schizo.client.feature.FeatureCategory;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Central registry for client-side modules. */
public final class FeatureRegistry {
    private static final Map<String, Feature> FEATURES = new LinkedHashMap<>();

    private FeatureRegistry() {}

    public static synchronized void register(Feature feature) {
        if (feature == null) throw new IllegalArgumentException("Feature cannot be null");
        if (FEATURES.putIfAbsent(feature.id(), feature) != null) {
            throw new IllegalStateException("Duplicate feature id: " + feature.id());
        }
    }

    public static synchronized Optional<Feature> get(String id) {
        return Optional.ofNullable(FEATURES.get(id));
    }

    public static synchronized Collection<Feature> all() {
        return Collections.unmodifiableList(List.copyOf(FEATURES.values()));
    }

    public static synchronized List<Feature> inCategory(FeatureCategory category) {
        return FEATURES.values().stream().filter(feature -> feature.category() == category).toList();
    }

    public static synchronized void registerDefaults() {
        SchizoClient.LOGGER.info("Schizo module registry ready ({} modules)", FEATURES.size());
    }
}
