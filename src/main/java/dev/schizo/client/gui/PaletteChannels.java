package dev.schizo.client.gui;

import java.awt.Color;

/** Pure channel operations shared by the detached picker. */
public final class PaletteChannels {
   private PaletteChannels() {}

   public static int fromPointer(double pointer, int left, int width) {
      return fromPointer(pointer,left,width,255);
   }

   public static int fromPointer(double pointer, int left, int width, int maximum) {
      if (!Double.isFinite(pointer) || width <= 1 || maximum <= 0) return 0;
      return (int)Math.round(maximum * Math.max(0.0, Math.min(1.0, (pointer - left) / (width - 1))));
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

   /** Allocation-free gradient sample with the same channel values as replace(). */
   public static int gradientArgb(int argb, int channel, int value) {
      int shift = switch (channel) {
         case 0 -> 16;
         case 1 -> 8;
         case 2 -> 0;
         case 3 -> 24;
         default -> throw new IllegalArgumentException("Invalid channel");
      };
      int v = Math.max(0, Math.min(255, value));
      int sample = (argb & ~(255 << shift)) | (v << shift);
      return channel == 3 ? sample : sample | 0xff000000;
   }
}
