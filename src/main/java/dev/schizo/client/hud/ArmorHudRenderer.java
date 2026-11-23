package dev.schizo.client.hud;

import dev.schizo.client.FeatureRegistry;
import dev.schizo.client.feature.ArmorHudFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Renders armor icons and durability as a compact side panel. */
public final class ArmorHudRenderer {
    private static final int PANEL_COLOR = 0xA0101520;
    private static final int ACCENT_COLOR = 0xFF55DDEE;
    private static final int TEXT_COLOR = 0xFFF1F4F8;
    private static final List<SlotLabel> SLOTS = List.of(
        new SlotLabel(EquipmentSlot.HEAD, "HEAD"),
        new SlotLabel(EquipmentSlot.CHEST, "CHEST"),
        new SlotLabel(EquipmentSlot.LEGS, "LEGS"),
        new SlotLabel(EquipmentSlot.FEET, "FEET")
    );

    private ArmorHudRenderer() {}

    public static void render(GuiGraphics graphics, Minecraft client) {
        var registered = FeatureRegistry.get("armor-hud").orElse(null);
        if (!(registered instanceof ArmorHudFeature feature) || !feature.isEnabled() || client.player == null) return;

        int visibleSlots = 0;
        for (SlotLabel slot : SLOTS) {
            if (feature.showEmptySlots().get() || !client.player.getItemBySlot(slot.slot()).isEmpty()) {
                visibleSlots++;
            }
        }
        if (visibleSlots == 0) return;

        int guiWidth = client.getWindow().getGuiScaledWidth();
        int guiHeight = client.getWindow().getGuiScaledHeight();
        int panelWidth = 112;
        int panelHeight = visibleSlots * 20 + 8;
        int left = feature.position().get().equals("Right") ? guiWidth - panelWidth - 6 : 6;
        int top = guiHeight - panelHeight - 36;
        int textX = left + 31;

        graphics.fill(left, top, left + panelWidth, top + panelHeight, PANEL_COLOR);
        graphics.fill(feature.position().get().equals("Right") ? left + panelWidth - 2 : left,
            top, feature.position().get().equals("Right") ? left + panelWidth : left + 2,
            top + panelHeight, ACCENT_COLOR);

        int row = 0;
        for (SlotLabel slot : SLOTS) {
            ItemStack stack = client.player.getItemBySlot(slot.slot());
            if (stack.isEmpty() && !feature.showEmptySlots().get()) continue;

            int y = top + 4 + row * 20;
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, left + 7, y);
                graphics.renderItemDecorations(client.font, stack, left + 7, y);
            }

            String label;
            if (stack.isEmpty()) {
                label = slot.label() + "  EMPTY";
            } else if (feature.showDurability().get() && stack.isDamageableItem()) {
                int remaining = Math.max(0, 100 - (stack.getDamageValue() * 100 / stack.getMaxDamage()));
                label = slot.label() + "  " + remaining + "%";
            } else {
                label = slot.label();
            }

            int color = stack.isEmpty() ? 0xFF8A929C : TEXT_COLOR;
            graphics.drawString(client.font, label, textX, y + 5, color, true);
            row++;
        }
    }

    private record SlotLabel(EquipmentSlot slot, String label) {}
}
