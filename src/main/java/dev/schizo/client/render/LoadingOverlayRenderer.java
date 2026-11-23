/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class LoadingOverlayRenderer {

   private static final String[] SCHIZO_ASCII = {
      "#####  #####  #...#  #####  #####  #####",
      "#....  #....  #...#  ..#..  ...#.  #...#",
      "#####  #....  #####  ..#..  ..#..  #...#",
      "....#  #....  #...#  ..#..  .#...  #...#",
      "#####  #####  #...#  #####  #####  #####"
   };

   private LoadingOverlayRenderer() {
   }

   public static void render(GuiGraphics graphics, float opacity, float backgroundOpacity) {
      render(graphics, opacity, backgroundOpacity, 0.75F);
   }

   public static void render(
      GuiGraphics graphics, float opacity, float backgroundOpacity, float logoScale
   ) {
      float fade = Mth.clamp(opacity, 0.0F, 1.0F);
      int screenWidth = GuiScaleUtils.getScaledWidth();
      int screenHeight = GuiScaleUtils.getScaledHeight();
      Font font = Minecraft.getInstance().font;

      int lineHeight = font.lineHeight + 2;
      int logoWidth = 0;
      for (String row : SCHIZO_ASCII) {
         logoWidth = Math.max(logoWidth, font.width(row));
      }

      float scale = Mth.clamp(logoScale / 0.75F, 0.8F, 1.35F);
      int scaledLogoWidth = (int)(logoWidth * scale);
      int logoHeight = (int)(SCHIZO_ASCII.length * lineHeight * scale);
      int frameWidth = Math.min(screenWidth - 24, Math.max(320, scaledLogoWidth + 64));
      int frameHeight = logoHeight + 74;
      int frameLeft = (screenWidth - frameWidth) / 2;
      int frameTop = (screenHeight - frameHeight) / 2;
      int red = withOpacity(0xFFE51B37, fade);
      int mutedRed = withOpacity(0xFF9B3446, fade);
      int panel = withOpacity(0xFF0D080D, fade);
      int background = withOpacity(0xFF050509, fade * Mth.clamp(backgroundOpacity, 0.0F, 1.0F));

      graphics.fill(0, 0, screenWidth, screenHeight, background);
      graphics.fill(frameLeft, frameTop, frameLeft + frameWidth, frameTop + frameHeight, panel);
      graphics.fill(frameLeft, frameTop, frameLeft + frameWidth, frameTop + 2, red);
      graphics.fill(frameLeft, frameTop + frameHeight - 2, frameLeft + frameWidth, frameTop + frameHeight, red);
      graphics.fill(frameLeft, frameTop, frameLeft + 2, frameTop + frameHeight, mutedRed);
      graphics.fill(frameLeft + frameWidth - 2, frameTop, frameLeft + frameWidth, frameTop + frameHeight, mutedRed);

      int centerX = screenWidth / 2;
      int logoTop = frameTop + 18;
      graphics.pose().pushMatrix();
      graphics.pose().translate(centerX, logoTop);
      graphics.pose().scale(scale, scale);
      for (int rowIndex = 0; rowIndex < SCHIZO_ASCII.length; rowIndex++) {
         String row = SCHIZO_ASCII[rowIndex];
         graphics.drawString(font, row, -font.width(row) / 2, rowIndex * lineHeight, red, true);
      }
      graphics.pose().popMatrix();

      int separatorY = logoTop + logoHeight + 9;
      graphics.fill(frameLeft + 28, separatorY, frameLeft + frameWidth - 28, separatorY + 1, mutedRed);

      String tagline = "[ SCHIZO CLIENT // FABRIC 1.21.11 ]";
      graphics.drawString(font, tagline, centerX - font.width(tagline) / 2, separatorY + 10, mutedRed, false);

      String footer = "INITIALIZING CLIENT SYSTEMS";
      graphics.drawString(font, footer, centerX - font.width(footer) / 2, separatorY + 25, red, false);
   }

   private static int withOpacity(int color, float opacity) {
      int alpha = Math.round(Mth.clamp(opacity, 0.0F, 1.0F) * 255.0F);
      return (color & 0x00FFFFFF) | (alpha << 24);
   }
}
