/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.render;

import dev.schizo.client.gui.UiMotionPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Crimson square-and-compasses startup identity; uses only built-in GUI primitives. */
public final class LoadingOverlayRenderer {
   private LoadingOverlayRenderer() {}

   public static void render(GuiGraphics graphics, float opacity, float backgroundOpacity) {
      render(graphics, opacity, backgroundOpacity, 0.75F);
   }

   public static void render(GuiGraphics graphics, float opacity, float backgroundOpacity, float logoScale) {
      float fade = Mth.clamp(opacity, 0.0F, 1.0F);
      int width = graphics.guiWidth();
      int height = graphics.guiHeight();
      Font font = Minecraft.getInstance().font;
      int cx = width / 2;
      int cy = height / 2 - 12;
      int radius = Math.max(1, Math.min(45, Math.min(width / 4, Math.max(1, (height - 70) / 3))));
      if (!UiMotionPolicy.isReducedMotion()) radius = Math.max(1, (int)(radius * Mth.clamp(logoScale / 0.75F, 0.85F, 1.0F)));
      int bright = withOpacity(0xFFE7394D, fade);
      int muted = withOpacity(0xFF9F9FAA, fade);
      int graphite = withOpacity(0xFF181920, fade);
      int glow = withOpacity(0xFF44141E, fade);
      graphics.fill(0, 0, width, height, withOpacity(0xFF090A0E, fade * Mth.clamp(backgroundOpacity, 0.0F, 1.0F)));
      if (width < 64 || height < 64) return;
      int frame = radius + 10;
      graphics.fill(cx - frame, cy - frame, cx + frame, cy + frame, graphite);
      graphics.fill(cx - frame, cy - frame, cx + frame, cy - frame + 1, glow);
      graphics.fill(cx - frame, cy + frame - 1, cx + frame, cy + frame, glow);
      graphics.fill(cx - frame, cy - frame, cx - frame + 14, cy - frame + 2, bright);
      graphics.fill(cx + frame - 14, cy + frame - 2, cx + frame, cy + frame, bright);
      int stroke = Math.max(1, radius / 11);
      int hingeY = cy - radius * 3 / 4;
      int legY = cy + radius * 2 / 3;
      int legX = radius * 3 / 5;
      int squareX = radius * 4 / 5;
      int squareY = cy - radius / 7;
      int bottomY = cy + radius * 4 / 5;
      stroke(graphics, cx, hingeY, cx - legX, legY, stroke, bright);
      stroke(graphics, cx, hingeY, cx + legX, legY, stroke, bright);
      stroke(graphics, cx - squareX, squareY, cx, bottomY, stroke, bright);
      stroke(graphics, cx + squareX, squareY, cx, bottomY, stroke, bright);
      graphics.fill(cx - stroke, hingeY - stroke, cx + stroke + 1, hingeY + stroke + 1, bright);
      if (radius >= 18) graphics.drawString(font, "G", cx - font.width("G") / 2, cy - 2, bright, false);
      int textY = cy + frame + 12;
      if (textY + 23 < height) {
         String name = "S C H I Z O";
         graphics.drawString(font, name, cx - font.width(name) / 2, textY, bright, false);
         String status = "LOADING RESOURCES";
         graphics.drawString(font, status, cx - font.width(status) / 2, textY + 14, muted, false);
      }
   }

   private static void stroke(GuiGraphics graphics, int x0, int y0, int x1, int y1, int thickness, int color) {
      int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
      int half = thickness / 2;
      for (int i = 0; i <= steps; i++) {
         float fraction = steps == 0 ? 0.0F : (float)i / steps;
         int x = Math.round(x0 + (x1 - x0) * fraction);
         int y = Math.round(y0 + (y1 - y0) * fraction);
         graphics.fill(x - half, y - half, x - half + thickness, y - half + thickness, color);
      }
   }

   private static int withOpacity(int color, float opacity) {
      return (color & 0x00FFFFFF) | (Math.round(Mth.clamp(opacity, 0.0F, 1.0F) * 255.0F) << 24);
   }
}
