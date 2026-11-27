package dev.schizo.client.gui.screens;

import dev.schizo.client.utils.FloatAnimation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/** Native text editing with a palette-aware perimeter. Native text layout stays untouched. */
final class StudioEditBox extends EditBox {
   private final FloatAnimation focusMotion = new FloatAnimation();

   StudioEditBox(Font font, int x, int y, int width, int height, Component message) {
      super(font, x, y, width, height, message);
   }

   @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      super.renderWidget(graphics, mouseX, mouseY, partialTick);
      int x = getX(), y = getY(), w = getWidth(), h = getHeight();
      if (w < 2 || h < 2) return;
      StudioScreenTheme theme = StudioScreenTheme.current();
      focusMotion.setTarget(isFocused(), 120L);
      int edge = StudioScreenTheme.blend(0xff444651, theme.accent(), focusMotion.getValue());
      graphics.fill(x, y, x + w, y + 1, edge);
      graphics.fill(x, y + h - 1, x + w, y + h, edge);
      graphics.fill(x, y + 1, x + 1, y + h - 1, edge);
      graphics.fill(x + w - 1, y + 1, x + w, y + h - 1, edge);
   }
}
