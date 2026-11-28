package dev.schizo.client.gui.screens;

import dev.schizo.client.utils.FloatAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import java.time.Duration;
import java.util.Objects;
import net.minecraft.network.chat.Component;

/** Palette-aware button; vanilla Button retains input, focus, sound and narration. */
final class StudioButton extends Button {
   private final FloatAnimation hover = new FloatAnimation();
   private String help, installedTooltip;

   StudioButton(int x, int y, int width, int height, Component label, OnPress action) {
      super(x, y, width, height, label, action, DEFAULT_NARRATION);
      setTooltipDelay(Duration.ofMillis(450));
   }

   StudioButton help(String text) {
      help = text;
      return this;
   }

   private void updateTooltip(String label, boolean clipped) {
      String text = StudioTooltipText.resolve(label, clipped, help);
      if (Objects.equals(installedTooltip, text)) return;
      installedTooltip = text;
      setTooltip(text == null ? null : Tooltip.create(Component.literal(text)));
   }

   @Override protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      int x = getX(), y = getY(), w = getWidth(), h = getHeight();
      if (w <= 0 || h <= 0) return;
      StudioScreenTheme theme = StudioScreenTheme.current();
      hover.setTarget(active && (isHoveredOrFocused()), 120L);
      float amount = active ? hover.getValue() : 0;
      int surface = StudioScreenTheme.blend(theme.row(), theme.hover(), amount);
      int edge = active ? StudioScreenTheme.blend(0xff444651, theme.accent(), amount) : 0xff303139;
      if (!active) surface = StudioScreenTheme.blend(theme.panel(), theme.row(), 0.4);
      graphics.fill(x, y, x + w, y + h, edge);
      if (w > 2 && h > 2) graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, surface);
      if (active && isFocused() && w > 4 && h > 3) graphics.fill(x + 2, y + h - 2, x + w - 2, y + h - 1, theme.accent());
      var font = Minecraft.getInstance().font;
      String label = getMessage().getString();
      int available = Math.max(0, w - 8);
      boolean clipped = font.width(label) > available || h < font.lineHeight;
      updateTooltip(label, clipped);
      if (font.width(label) > available) {
         String ellipsis = "…";
         label = available < font.width(ellipsis) ? "" : font.plainSubstrByWidth(label, available - font.width(ellipsis)) + ellipsis;
      }
      if (!label.isEmpty() && h >= font.lineHeight) {
         graphics.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - font.lineHeight) / 2 + 1,
            active ? 0xffeeeeef : 0xff9899a5, false);
      }
   }
}
