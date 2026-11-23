package dev.schizo.client.setting;

import java.util.List;

/** Single selection from a fixed set of string options. */
public final class ModeSetting extends Setting<String> {
    private final List<String> modes;

    public ModeSetting(String id, String name, List<String> modes, String defaultValue) {
        super(id, name, defaultValue);
        if (modes == null || modes.isEmpty() || modes.stream().anyMatch(s -> s == null || s.isBlank())) {
            throw new IllegalArgumentException("At least one non-empty mode is required");
        }
        this.modes = List.copyOf(modes);
        if (!this.modes.contains(defaultValue)) throw new IllegalArgumentException("Default mode is not in options");
    }

    @Override
    protected String validate(String value) {
        if (modes == null) return value;
        if (!modes.contains(value)) throw new IllegalArgumentException("Unsupported mode: " + value);
        return value;
    }

    public List<String> modes() { return modes; }
}
