package dev.schizo.client.hud;

import dev.schizo.client.FeatureRegistry;
import dev.schizo.client.feature.EffectsHudFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.Comparator;
import java.util.List;

/** Renders active player status effects in a compact right-side list. */
public final class EffectsHudRenderer {
    private static final int PANEL_COLOR = 0xA0101520;
    private static final int ACCENT_COLOR = 0xFF55DDEE;
    private static final int TEXT_COLOR = 0xFFF1F4F8;

    private EffectsHudRenderer() {}

    public static void render(GuiGraphics graphics, Minecraft client) {
        var registered = FeatureRegistry.get("effects-hud").orElse(null);
        if (!(registered instanceof EffectsHudFeature feature) || !feature.isEnabled() || client.player == null) return;

        List<MobEffectInstance> effects = client.player.getActiveEffects().stream()
            .sorted(Comparator.comparing(effect -> effect.getEffect().value().getDisplayName().getString()))
            .toList();

        if (effects.isEmpty()) return;

        int maxRows = (int) Math.round(feature.maxRows().get());
        int rows = Math.min(maxRows, effects.size());
        List<String> labels = effects.stream().limit(rows)
            .map(effect -> format(effect, feature))
            .toList();

        int panelWidth = Math.max(client.font.width("ACTIVE EFFECTS"), labels.stream()
            .mapToInt(client.font::width).max().orElse(0)) + 16;
        int panelHeight = 22 + rows * 13;
        int right = client.getWindow().getGuiScaledWidth() - 6;
        int left = right - panelWidth;
        int top = Math.max(6, (client.getWindow().getGuiScaledHeight() - panelHeight) / 2);

        graphics.fill(left, top, right, top + panelHeight, PANEL_COLOR);
        graphics.fill(right - 2, top, right, top + panelHeight, ACCENT_COLOR);
        graphics.drawString(client.font, "ACTIVE EFFECTS", left + 7, top + 6, ACCENT_COLOR, true);

        int y = top + 20;
        for (int i = 0; i < rows; i++) {
            MobEffectInstance effect = effects.get(i);
            int effectColor = 0xFF000000 | (effect.getEffect().value().getColor() & 0x00FFFFFF);
            graphics.drawString(client.font, labels.get(i), left + 7, y, effectColor == 0xFF000000 ? TEXT_COLOR : effectColor, true);
            y += 13;
        }

        if (effects.size() > rows) {
            String overflow = "+" + (effects.size() - rows) + " more";
            graphics.drawString(client.font, overflow, left + 7, y - 1, 0xFF9AA7B5, true);
        }
    }

    private static String format(MobEffectInstance effect, EffectsHudFeature feature) {
        StringBuilder label = new StringBuilder(effect.getEffect().value().getDisplayName().getString());
        if (feature.showAmplifier().get()) {
            label.append(' ').append(amplifier(effect.getAmplifier()));
        }
        if (feature.showDuration().get()) {
            label.append("  ").append(duration(effect));
        }
        return label.toString();
    }

    private static String duration(MobEffectInstance effect) {
        if (effect.isInfiniteDuration()) return "∞";
        int seconds = Math.max(0, effect.getDuration() / 20);
        return String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    private static String amplifier(int amplifier) {
        return switch (amplifier) {
            case 0 -> "I";
            case 1 -> "II";
            case 2 -> "III";
            case 3 -> "IV";
            case 4 -> "V";
            default -> "x" + (amplifier + 1);
        };
    }
}
