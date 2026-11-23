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
      "   ██████  ▄████▄   ██░ ██  ██▓▒███████▒ ▒█████  ",
      "▒██    ▒ ▒██▀ ▀█  ▓██░ ██▒▓██▒▒ ▒ ▒ ▄▀░▒██▒  ██▒",
      "░ ▓██▄   ▒▓█    ▄ ▒██▀▀██░▒██▒░ ▒ ▄▀▒░ ▒██░  ██▒",
      "  ▒   ██▒▒▓▓▄ ▄██▒░▓█ ░██ ░██░  ▄▀▒   ░▒██   ██░",
      "▒██████▒▒▒ ▓███▀ ░▓█▒░██▓░██░▒███████▒░ ████▓▒░",
      "▒ ▒▓▒ ▒ ░░ ░▒ ▒  ░ ▒ ░▒░▒░▓  ░▒▒ ▓░▒░▒░ ▒░▒░▒░ ",
      "░ ░▒  ░ ░  ░  ▒    ▒ ░▒░ ░ ▒ ░░▒ ▒ ░ ▒  ░ ▒ ▒░ ",
      "░  ░  ░  ░         ░  ░░ ░ ▒ ░░ ░ ░ ░ ░░ ░ ░ ▒  ",
      "      ░  ░ ░       ░  ░  ░ ░    ░ ░        ░ ░  ",
      "         ░                    ░                    "
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

      int lineHeight = font.lineHeight + 1;
      int logoWidth = 0;
      for (String row : SCHIZO_ASCII) {
         logoWidth = Math.max(logoWidth, font.width(row));
      }

      float requestedScale = Mth.clamp(logoScale / 0.75F, 0.85F, 1.25F);
      float widthScale = (screenWidth - 32.0F) / Math.max(logoWidth, 1);
      float heightScale = (screenHeight - 104.0F) / (SCHIZO_ASCII.length * lineHeight);
      float scale = Math.max(0.55F, Math.min(requestedScale, Math.min(widthScale, heightScale)));
      int scaledLogoWidth = (int)(logoWidth * scale);
      int logoHeight = (int)(SCHIZO_ASCII.length * lineHeight * scale);
      int frameWidth = Math.min(screenWidth - 16, scaledLogoWidth + 48);
      int frameHeight = logoHeight + 76;
      int frameLeft = (screenWidth - frameWidth) / 2;
      int frameTop = Math.max(8, (screenHeight - frameHeight) / 2);

      int accent = withOpacity(0xFFC794F4, fade);
      int glow = withOpacity(0xFF6C438A, fade * 0.65F);
      int muted = withOpacity(0xFF9875B7, fade * 0.85F);
      int panel = withOpacity(0xFF100B18, fade);
      int background = withOpacity(0xFF07050C, fade * Mth.clamp(backgroundOpacity, 0.0F, 1.0F));

      graphics.fill(0, 0, screenWidth, screenHeight, background);
      graphics.fill(frameLeft, frameTop, frameLeft + frameWidth, frameTop + frameHeight, panel);
      graphics.fill(frameLeft, frameTop, frameLeft + frameWidth, frameTop + 2, accent);
      graphics.fill(frameLeft, frameTop + frameHeight - 2, frameLeft + frameWidth, frameTop + frameHeight, accent);
      graphics.fill(frameLeft, frameTop, frameLeft + 2, frameTop + frameHeight, glow);
      graphics.fill(frameLeft + frameWidth - 2, frameTop, frameLeft + frameWidth, frameTop + frameHeight, glow);

      int centerX = screenWidth / 2;
      int logoTop = frameTop + 15;
      graphics.pose().pushMatrix();
      graphics.pose().translate(centerX, logoTop);
      graphics.pose().scale(scale, scale);
      for (int rowIndex = 0; rowIndex < SCHIZO_ASCII.length; rowIndex++) {
         String row = SCHIZO_ASCII[rowIndex];
         int textX = -font.width(row) / 2;
         int textY = rowIndex * lineHeight;
         graphics.drawString(font, row, textX + 1, textY + 1, glow, false);
         graphics.drawString(font, row, textX, textY, accent, false);
      }
      graphics.pose().popMatrix();

      int separatorY = logoTop + logoHeight + 7;
      graphics.fill(frameLeft + 22, separatorY, frameLeft + frameWidth - 22, separatorY + 1, glow);

      String tagline = "[ SCHIZO CLIENT // FABRIC 1.21.11 ]";
      graphics.drawString(font, tagline, centerX - font.width(tagline) / 2, separatorY + 9, muted, false);

      String footer = "INITIALIZING CLIENT SYSTEMS";
      graphics.drawString(font, footer, centerX - font.width(footer) / 2, separatorY + 24, accent, false);
   }

   private static int withOpacity(int color, float opacity) {
      int alpha = Math.round(Mth.clamp(opacity, 0.0F, 1.0F) * 255.0F);
      return (color & 0x00FFFFFF) | (alpha << 24);
   }
}
