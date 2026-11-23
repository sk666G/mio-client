package dev.schizo.client.hud;

import dev.schizo.client.FeatureRegistry;
import dev.schizo.client.feature.Feature;
import dev.schizo.client.feature.HudFeature;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Registers Schizo's optional informational HUD overlay. */
public final class HudOverlay {
    private static final int PANEL_COLOR = 0xA0101520;
    private static final int ACCENT_COLOR = 0xFF55DDEE;
    private static final int TEXT_COLOR = 0xFFF1F4F8;

    private HudOverlay() {}

    public static void register() {
        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath("schizo", "information_hud"),
            HudOverlay::render
        );
    }

    private static void render(GuiGraphics graphics, net.minecraft.client.DeltaTracker tickCounter) {
        Feature registered = FeatureRegistry.get("information-hud").orElse(null);
        if (!(registered instanceof HudFeature feature) || !feature.isEnabled()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) return;

        List<String> infoLines = new ArrayList<>();
        if (feature.showCoordinates().get()) {
            var position = client.player.blockPosition();
            infoLines.add("XYZ " + position.getX() + " " + position.getY() + " " + position.getZ());
        }
        if (feature.showDirection().get()) {
            infoLines.add("Facing " + client.player.getDirection().getName().toUpperCase(java.util.Locale.ROOT));
        }
        if (feature.showFps().get()) {
            infoLines.add("FPS " + client.getFps());
        }

        int panelWidth = Math.max(client.font.width("SCHIZO"), 54);
        for (String line : infoLines) panelWidth = Math.max(panelWidth, client.font.width(line));
        panelWidth += 12;

        int panelHeight = 22 + infoLines.size() * 12;
        graphics.fill(5, 5, 5 + panelWidth, 5 + panelHeight, PANEL_COLOR);
        graphics.fill(5, 5, 7, 5 + panelHeight, ACCENT_COLOR);
        graphics.drawString(client.font, Component.literal("SCHIZO"), 12, 10, ACCENT_COLOR, true);

        int y = 23;
        for (String line : infoLines) {
            graphics.drawString(client.font, Component.literal(line), 12, y, TEXT_COLOR, true);
            y += 12;
        }

        if (feature.showModules().get()) {
            List<Feature> enabled = FeatureRegistry.all().stream()
                .filter(Feature::isEnabled)
                .sorted(Comparator.comparingInt((Feature module) -> client.font.width(module.name())).reversed())
                .toList();

            if (!enabled.isEmpty()) {
                int right = client.getWindow().getGuiScaledWidth() - 6;
                int width = enabled.stream()
                    .mapToInt(module -> client.font.width(module.name()))
                    .max().orElse(0) + 14;
                int height = 8 + enabled.size() * 13;
                int left = right - width;
                graphics.fill(left, 5, right, 5 + height, PANEL_COLOR);
                graphics.fill(right - 2, 5, right, 5 + height, ACCENT_COLOR);

                int moduleY = 9;
                for (Feature module : enabled) {
                    int textX = right - 8 - client.font.width(module.name());
                    graphics.drawString(client.font, Component.literal(module.name()),
                        textX, moduleY, ACCENT_COLOR, true);
                    moduleY += 13;
                }
            }
        }
    }
}
