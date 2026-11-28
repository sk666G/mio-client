package dev.schizo.client.gui;

import java.awt.Color;

/** Detached HSV state retains hue even at zero saturation or brightness. */
public record PaletteHsv(float hue, float saturation, float value, int alpha) {
   public PaletteHsv {
      hue = unit(hue);
      saturation = unit(saturation);
      value = unit(value);
      alpha = Math.max(0, Math.min(255, alpha));
   }

   private static float unit(float value) {
      if (!Float.isFinite(value)) throw new IllegalArgumentException("Non-finite HSV value");
      return Math.max(0F, Math.min(1F, value));
   }

   public static PaletteHsv from(Color color) {
      float[] hsv = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
      return new PaletteHsv(hsv[0], hsv[1], hsv[2], color.getAlpha());
   }

   public static int maximum(int channel) {
      return switch (channel) {
         case 0 -> 360;
         case 1, 2 -> 100;
         case 3 -> 255;
         default -> throw new IllegalArgumentException("Invalid HSV channel");
      };
   }

   public int get(int channel) {
      return switch (channel) {
         case 0 -> Math.round(hue * 360);
         case 1 -> Math.round(saturation * 100);
         case 2 -> Math.round(value * 100);
         case 3 -> alpha;
         default -> throw new IllegalArgumentException("Invalid HSV channel");
      };
   }

   public PaletteHsv with(int channel, int proposed) {
      int bounded = Math.max(0, Math.min(maximum(channel), proposed));
      return switch (channel) {
         case 0 -> new PaletteHsv(bounded / 360F, saturation, value, alpha);
         case 1 -> new PaletteHsv(hue, bounded / 100F, value, alpha);
         case 2 -> new PaletteHsv(hue, saturation, bounded / 100F, alpha);
         case 3 -> new PaletteHsv(hue, saturation, value, bounded);
         default -> throw new IllegalArgumentException("Invalid HSV channel");
      };
   }

   public Color color() { return new Color(argb(hue, saturation, value, alpha), true); }

   private static int argb(float hue, float saturation, float value, int alpha) {
      return (Color.HSBtoRGB(hue, saturation, value) & 0x00ffffff) | (alpha << 24);
   }

   /** RGB rails are opaque; alpha rail retains the selected RGB color. */
   public int gradientArgb(int channel, int proposed) {
      int bounded = Math.max(0, Math.min(maximum(channel), proposed));
      return switch (channel) {
         case 0 -> argb(bounded / 360F, 1F, 1F, 255);
         case 1 -> argb(hue, bounded / 100F, value, 255);
         case 2 -> argb(hue, saturation, bounded / 100F, 255);
         case 3 -> argb(hue, saturation, value, bounded);
         default -> throw new IllegalArgumentException("Invalid HSV channel");
      };
   }
}
