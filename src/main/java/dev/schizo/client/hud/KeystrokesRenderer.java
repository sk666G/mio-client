package dev.schizo.client.hud;

import dev.schizo.client.FeatureRegistry;
import dev.schizo.client.feature.KeystrokesFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Draws a compact live keyboard display. */
public final class KeystrokesRenderer {
    private static final int PANEL_COLOR = 0xA0101520;
    private static final int KEY_IDLE = 0xFF26323F;
    private static final int KEY_DOWN = 0xFF176477;
    private static final int KEY_TEXT = 0xFFF1F4F8;

    private KeystrokesRenderer() {}

    public static void render(GuiGraphics graphics, Minecraft client) {
        var registered = FeatureRegistry.get("keystrokes").orElse(null);
        if (!(registered instanceof KeystrokesFeature feature) || !feature.isEnabled() || client.player == null) return;

        boolean showJump = feature.showJump().get();
        int panelWidth = 62;
        int panelHeight = showJump ? 66 : 48;
        int guiWidth = client.getWindow().getGuiScaledWidth();
        int guiHeight = client.getWindow().getGuiScaledHeight();
        boolean right = feature.position().get().equals("Right");
        int left = right ? guiWidth - panelWidth - 6 : 6;
        int top = guiHeight - panelHeight - 38;

        graphics.fill(left, top, left + panelWidth, top + panelHeight, PANEL_COLOR);
        drawKey(graphics, client, "W", client.options.keyUp.isDown(), left + 23, top + 4, 16, 16);
        drawKey(graphics, client, "A", client.options.keyLeft.isDown(), left + 5, top + 22, 16, 16);
        drawKey(graphics, client, "S", client.options.keyDown.isDown(), left + 23, top + 22, 16, 16);
        drawKey(graphics, client, "D", client.options.keyRight.isDown(), left + 41, top + 22, 16, 16);

        if (showJump) {
            boolean down = client.options.keyJump.isDown();
            graphics.fill(left + 5, top + 42, left + 57, top + 60, down ? KEY_DOWN : KEY_IDLE);
            graphics.drawCenteredString(client.font, Component.literal("JUMP"), left + 31, top + 47, KEY_TEXT);
        }
    }

    private static void drawKey(GuiGraphics graphics, Minecraft client, String label,
                                boolean down, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, down ? KEY_DOWN : KEY_IDLE);
        graphics.drawCenteredString(client.font, Component.literal(label), x + width / 2, y + 4, KEY_TEXT);
    }
}
