package dev.schizo.client.setting;

import java.util.Objects;

/** A validated, serializable-in-principle value owned by a feature. */
public abstract class Setting<T> {
    private final String id;
    private final String name;
    private final T defaultValue;
    private T value;

    protected Setting(String id, String name, T defaultValue) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Setting id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Setting name is required");
        this.id = id;
        this.name = name;
        this.defaultValue = validate(Objects.requireNonNull(defaultValue, "defaultValue"));
        this.value = this.defaultValue;
    }

    protected abstract T validate(T value);

    public final synchronized void set(T value) {
        this.value = validate(Objects.requireNonNull(value, "value"));
    }

    public final synchronized void reset() { value = defaultValue; }
    public final String id() { return id; }
    public final String name() { return name; }
    public final T defaultValue() { return defaultValue; }
    public final synchronized T get() { return value; }
}
