package dev.schizo.client.setting;

/** Bounded numeric setting. Values are clamped to the declared range. */
public final class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;

    public NumberSetting(String id, String name, double defaultValue, double min, double max, double step) {
        super(id, name, defaultValue);
        if (!Double.isFinite(min) || !Double.isFinite(max) || min > max) {
            throw new IllegalArgumentException("Invalid numeric range");
        }
        if (!Double.isFinite(step) || step <= 0) throw new IllegalArgumentException("Step must be positive");
        this.min = min;
        this.max = max;
        this.step = step;
        set(defaultValue);
    }

    @Override
    protected Double validate(Double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Value must be finite");
        return Math.max(min, Math.min(max, value));
    }

    public double min() { return min; }
    public double max() { return max; }
    public double step() { return step; }
}
