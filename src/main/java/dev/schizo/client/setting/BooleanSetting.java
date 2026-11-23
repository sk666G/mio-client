package dev.schizo.client.setting;

/** On/off setting. */
public final class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String id, String name, boolean defaultValue) {
        super(id, name, defaultValue);
    }

    @Override
    protected Boolean validate(Boolean value) { return value; }
}
