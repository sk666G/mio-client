package dev.schizo.client.feature;

import dev.schizo.client.setting.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Base class for an opt-in client-side module. */
public abstract class Feature {
    private final String id;
    private final String name;
    private final String description;
    private final FeatureCategory category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled;
    private int keyCode;

    protected Feature(String id, String name, String description, FeatureCategory category) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Feature id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Feature name is required");
        this.id = id;
        this.name = name;
        this.description = Objects.requireNonNull(description, "description");
        this.category = Objects.requireNonNull(category, "category");
    }

    public final synchronized void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        try {
            if (enabled) onEnable();
            else onDisable();
        } catch (RuntimeException | Error exception) {
            this.enabled = !enabled;
            throw exception;
        }
    }

    public final void toggle() {
        setEnabled(!isEnabled());
    }

    protected void onEnable() {}
    protected void onDisable() {}

    protected final <T extends Setting<?>> T addSetting(T setting) {
        Objects.requireNonNull(setting, "setting");
        if (settings.stream().anyMatch(existing -> existing.id().equals(setting.id()))) {
            throw new IllegalArgumentException("Duplicate setting id: " + setting.id());
        }
        settings.add(setting);
        return setting;
    }

    public final String id() { return id; }
    public final String name() { return name; }
    public final String description() { return description; }
    public final FeatureCategory category() { return category; }
    public final synchronized boolean isEnabled() { return enabled; }
    public final int keyCode() { return keyCode; }
    public final void setKeyCode(int keyCode) {
        if (keyCode < 0) throw new IllegalArgumentException("Key code cannot be negative");
        this.keyCode = keyCode;
    }
    public final List<Setting<?>> settings() {
        return Collections.unmodifiableList(settings);
    }
}
