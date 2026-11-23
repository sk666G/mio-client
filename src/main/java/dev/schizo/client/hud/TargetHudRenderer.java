package dev.schizo.client.hud;

import dev.schizo.client.FeatureRegistry;
import dev.schizo.client.feature.TargetHudFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

import java.util.Locale;

/** Draws a compact health panel for the living entity currently targeted by the crosshair. */
public final class TargetHudRenderer {
    private static final int PANEL_COLOR = 0xC0101520;
    private static final int ACCENT_COLOR = 0xFF55DDEE;
    private static final int TEXT_COLOR = 0xFFF1F4F8;
    private static final int TRACK_COLOR = 0xFF303A46;

    private TargetHudRenderer() {}

    public static void render(GuiGraphics graphics, Minecraft client) {
        var registered = FeatureRegistry.get("target-hud").orElse(null);
        if (!(registered instanceof TargetHudFeature feature)
                || !feature.isEnabled()
                || client.player == null
                || !(client.hitResult instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof LivingEntity target)
                || !target.isAlive()) {
            return;
        }

        float maxHealth = Math.max(1.0f, target.getMaxHealth());
        float health = Math.max(0.0f, Math.min(target.getHealth(), maxHealth));
        float healthRatio = health / maxHealth;

        String name = target.getDisplayName().getString();
        String healthText = String.format(Locale.ROOT, "%.1f / %.1f HP", health, maxHealth);
        String distanceText = feature.showDistance().get()
            ? String.format(Locale.ROOT, "%.1fm", client.player.distanceTo(target))
            : "";

        int contentWidth = Math.max(client.font.width(name), client.font.width(healthText));
        if (!distanceText.isEmpty()) contentWidth += client.font.width(distanceText) + 10;
        int width = Math.max(132, Math.min(260, contentWidth + 18));
        int height = 43;
        int left = (client.getWindow().getGuiScaledWidth() - width) / 2;
        int top = client.getWindow().getGuiScaledHeight() / 2 + 20;

        graphics.fill(left, top, left + width, top + height, PANEL_COLOR);
        graphics.fill(left, top, left + 2, top + height, ACCENT_COLOR);
        graphics.drawString(client.font, name, left + 8, top + 6, TEXT_COLOR, true);

        graphics.drawString(client.font, healthText, left + 8, top + 19, 0xFFB8C5D2, true);
        if (!distanceText.isEmpty()) {
            graphics.drawString(client.font, distanceText,
                left + width - 8 - client.font.width(distanceText), top + 19, ACCENT_COLOR, true);
        }

        int barLeft = left + 8;
        int barRight = left + width - 8;
        int barTop = top + 32;
        int barBottom = top + 37;
        graphics.fill(barLeft, barTop, barRight, barBottom, TRACK_COLOR);

        int healthColor = healthRatio > 0.6f ? 0xFF55D68B
            : healthRatio > 0.3f ? 0xFFFFC857 : 0xFFFF657A;
        int filledRight = barLeft + Math.round((barRight - barLeft) * healthRatio);
        if (filledRight > barLeft) {
            graphics.fill(barLeft, barTop, filledRight, barBottom, healthColor);
        }
    }
}
