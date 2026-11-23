package dev.schizo.client.hud;

import dev.schizo.client.FeatureRegistry;
import dev.schizo.client.feature.Feature;
import dev.schizo.client.feature.FeatureCategory;
import dev.schizo.client.feature.HudFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

/** Registers Schizo's optional informational HUD overlay. */
public final class HudOverlay {
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

        int y = 8;
        graphics.fill(5, 5, 154, 42, 0x90000000);
        graphics.drawString(client.font, Component.literal("Schizo"), 9, y, 0xFF55FFFF, true);
        y += 12;

        if (feature.showCoordinates().get()) {
            var position = client.player.blockPosition();
            String coordinates = "XYZ " + position.getX() + " " + position.getY() + " " + position.getZ();
            graphics.drawString(client.font, Component.literal(coordinates), 9, y, 0xFFFFFFFF, true);
            y += 12;
        }

        if (feature.showDirection().get()) {
            String direction = "Facing " + client.player.getDirection().getName().toUpperCase(java.util.Locale.ROOT);
            graphics.drawString(client.font, Component.literal(direction), 9, y, 0xFFFFFFFF, true);
        }
    }
}
