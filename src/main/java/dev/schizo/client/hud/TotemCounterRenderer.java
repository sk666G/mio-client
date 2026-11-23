package dev.schizo.client.hud;

import dev.schizo.client.FeatureRegistry;
import dev.schizo.client.feature.TotemCounterFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Draws a compact totem count near the top center of the game HUD. */
public final class TotemCounterRenderer {
    private static final int PANEL_COLOR = 0xA0101520;
    private static final int ACCENT_COLOR = 0xFF55DDEE;
    private static final int TEXT_COLOR = 0xFFF1F4F8;

    private TotemCounterRenderer() {}

    public static void render(GuiGraphics graphics, Minecraft client) {
        var registered = FeatureRegistry.get("totem-counter").orElse(null);
        if (!(registered instanceof TotemCounterFeature feature)
                || !feature.isEnabled() || client.player == null) {
            return;
        }

        int count = 0;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = client.player.getInventory().getItem(slot);
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                count += stack.getCount();
            }
        }
        ItemStack offhand = client.player.getOffhandItem();
        if (offhand.is(Items.TOTEM_OF_UNDYING)) {
            count += offhand.getCount();
        }
        if (count == 0 && !feature.showZero().get()) return;

        String label = count + " TOTEMS";
        int width = Math.max(76, client.font.width(label) + 34);
        int left = (client.getWindow().getGuiScaledWidth() - width) / 2;
        int top = 6;

        graphics.fill(left, top, left + width, top + 28, PANEL_COLOR);
        graphics.fill(left, top, left + 2, top + 28, ACCENT_COLOR);
        graphics.renderItem(new ItemStack(Items.TOTEM_OF_UNDYING), left + 7, top + 5);
        graphics.drawString(client.font, label, left + 31, top + 10, TEXT_COLOR, true);
    }
}
