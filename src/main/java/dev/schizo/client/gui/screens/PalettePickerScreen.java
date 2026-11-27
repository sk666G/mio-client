package dev.schizo.client.gui.screens;

import dev.schizo.client.gui.PaletteChannels;
import dev.schizo.client.gui.PaletteHex;
import java.awt.Color;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Pointer rails and native keyboard-focusable step buttons edit only a local color. */
final class PalettePickerScreen extends StudioScreen {
   private static final String[] CHANNELS = {"Red", "Green", "Blue", "Alpha"};
   private final Screen parent;
   private final String label;
   private final Consumer<Color> accepted;
   private Color color;
   private int left, top, panelWidth, railLeft, railWidth;
   private int dragging = -1;
   private boolean compact;

   PalettePickerScreen(Screen parent, String label, Color initial, Consumer<Color> accepted) {
      super(Component.literal("Pick " + label));
      this.parent = parent;
      this.label = label;
      this.color = initial;
      this.accepted = accepted;
   }

   @Override protected void init() {
      super.init();
      dragging = -1;
      panelWidth = Math.max(1, Math.min(340, width - 16));
      left = (width - panelWidth) / 2;
      top = Math.max(4, (height - 194) / 2);
      compact = panelWidth < 218 || height < 202;
      if (compact) {
         button("Back", left, Math.max(1, height - 25), panelWidth, this::onClose);
         return;
      }
      railLeft = left + 62;
      railWidth = panelWidth - 116;
      for (int i = 0; i < 4; i++) {
         int channel = i;
         int y = rowY(i);
         button(CHANNELS[i].substring(0, 1) + "-", left + panelWidth - 50, y, 23, () -> step(channel, -1));
         button(CHANNELS[i].substring(0, 1) + "+", left + panelWidth - 24, y, 23, () -> step(channel, 1));
      }
      int half = (panelWidth - 4) / 2;
      button("Use color", left, top + 169, half, () -> {
         accepted.accept(color);
         onClose();
      });
      button("Cancel", left + half + 4, top + 169, half, this::onClose);
   }

   private void button(String name, int x, int y, int w, Runnable action) {
      addRenderableWidget(new StudioButton(x, y, w, 18, Component.literal(name), button -> action.run()));
   }

   private int rowY(int channel) { return top + 53 + channel * 27; }

   private void step(int channel, int direction) {
      int amount = dev.schizo.client.utils.KeyUtils.isShiftDown() ? 16 : 1;
      color = PaletteChannels.replace(color, channel, PaletteChannels.get(color, channel) + direction * amount);
   }

   private void updateRail(double x) {
      color = PaletteChannels.replace(color, dragging, PaletteChannels.fromPointer(x, railLeft, railWidth));
   }

   @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (!compact && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT
         && event.x() >= railLeft && event.x() < railLeft + railWidth) {
         for (int i = 0; i < 4; i++) {
            if (event.y() >= rowY(i) && event.y() < rowY(i) + 18) {
               dragging = i;
               updateRail(event.x());
               return true;
            }
         }
      }
      return super.mouseClicked(event, doubleClick);
   }

   @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
      if (dragging >= 0 && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
         if (Double.isFinite(event.x())) updateRail(event.x());
         return true;
      }
      return super.mouseDragged(event, dx, dy);
   }

   @Override public boolean mouseReleased(MouseButtonEvent event) {
      if (dragging >= 0 && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
         if (Double.isFinite(event.x())) updateRail(event.x());
         dragging = -1;
         return true;
      }
      return super.mouseReleased(event);
   }

   private void checker(GuiGraphics graphics, int x, int y, int w, int h) {
      for (int iy = 0; iy < h; iy += 6) {
         for (int ix = 0; ix < w; ix += 6) {
            graphics.fill(x + ix, y + iy, x + Math.min(w, ix + 6), y + Math.min(h, iy + 6),
               ((ix / 6 + iy / 6) & 1) == 0 ? 0xff777781 : 0xff303139);
         }
      }
   }

   @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      StudioScreenTheme theme = StudioScreenTheme.current();
      graphics.fill(0, 0, width, height, theme.backdrop());
      if (compact) {
         graphics.drawString(font, font.plainSubstrByWidth("Reduce GUI scale to use picker", panelWidth), left, top, 0xffeeeeef, false);
      } else {
         graphics.fill(left - 4, top, left + panelWidth + 4, top + 194, theme.panel());
         graphics.fill(left - 4, top, left + panelWidth + 4, top + 1, theme.accent());
         graphics.drawString(font, "PICK / " + label, left, top + 7, theme.accent(), false);
         checker(graphics, left, top + 22, 48, 22);
         graphics.fill(left, top + 22, left + 48, top + 44, color.getRGB());
         graphics.drawString(font, PaletteHex.format(color), left + 56, top + 26, 0xffeeeeef, false);
         for (int i = 0; i < 4; i++) {
            int y = rowY(i), value = PaletteChannels.get(color, i);
            graphics.drawString(font, CHANNELS[i] + " " + value, left, y + 5, 0xffeeeeef, false);
            checker(graphics, railLeft, y + 3, railWidth, 12);
            for (int segment = 0; segment < 32; segment++) {
               int start = railLeft + segment * railWidth / 32, end = railLeft + (segment + 1) * railWidth / 32;
               Color sample = PaletteChannels.replace(color, i, Math.round(segment * 255.0F / 31));
               int argb = i == 3 ? sample.getRGB() : sample.getRGB() | 0xff000000;
               graphics.fill(start, y + 3, end, y + 15, argb);
            }
            int marker = railLeft + Math.round(value * (railWidth - 1) / 255.0F);
            graphics.fill(marker - 1, y + 1, marker + 2, y + 17, 0xff101116);
            graphics.fill(marker, y + 1, marker + 1, y + 17, 0xffeeeeef);
         }
      }
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   @Override public void onClose() { dragging = -1; minecraft.setScreen(parent); }
   @Override public boolean isPauseScreen() { return false; }
}
