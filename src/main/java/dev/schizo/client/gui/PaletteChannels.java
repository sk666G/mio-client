package dev.schizo.client.gui;

import java.awt.Color;

/** Pure channel operations shared by the detached picker. */
public final class PaletteChannels {
   private PaletteChannels() {}

   public static int fromPointer(double pointer, int left, int width) {
      if (!Double.isFinite(pointer) || width <= 1) return 0;
      return (int)Math.round(255 * Math.max(0.0, Math.min(1.0, (pointer - left) / (width - 1))));
   }

   public static Color replace(Color color, int channel, int value) {
      int v = Math.max(0, Math.min(255, value));
      return switch (channel) {
         case 0 -> new Color(v, color.getGreen(), color.getBlue(), color.getAlpha());
         case 1 -> new Color(color.getRed(), v, color.getBlue(), color.getAlpha());
         case 2 -> new Color(color.getRed(), color.getGreen(), v, color.getAlpha());
         case 3 -> new Color(color.getRed(), color.getGreen(), color.getBlue(), v);
         default -> throw new IllegalArgumentException("Invalid channel");
      };
   }

   public static int get(Color color, int channel) {
      return switch (channel) {
         case 0 -> color.getRed();
         case 1 -> color.getGreen();
         case 2 -> color.getBlue();
         case 3 -> color.getAlpha();
         default -> throw new IllegalArgumentException("Invalid channel");
      };
   }
}
