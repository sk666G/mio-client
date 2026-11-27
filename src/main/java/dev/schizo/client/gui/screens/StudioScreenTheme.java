package dev.schizo.client.gui.screens;

import dev.schizo.client.modules.client.UIModule;
import dev.schizo.client.modules.client.UiPalette;

/** Shared dark workspace chrome. Text accents are adjusted for contrast, not written back to settings. */
record StudioScreenTheme(int backdrop, int panel, int row, int hover, int selected, int accent) {
   private static UiPalette lastPalette;
   private static StudioScreenTheme lastTheme;

   // All screen rendering is on the client thread.
   static StudioScreenTheme current() {
      UiPalette palette = UIModule.INSTANCE.snapshotPalette();
      if (!palette.equals(lastPalette)) {
         lastTheme = from(palette);
         lastPalette = palette;
      }
      return lastTheme;
   }

   static StudioScreenTheme from(UiPalette palette) {
      int panel = blend(0xff13141a, palette.background().getRGB(), 0.32);
      int row = blend(0xff21222a, palette.button().getRGB(), 0.22);
      int hover = blend(row, 0xffeeeeef, 0.08);
      int selected = blend(row, palette.accent().getRGB(), 0.16);
      int accent = palette.accent().getRGB() | 0xff000000;
      // Enforce readable title text even for very dark custom accents.
      for (int i = 0; i < 32 && contrast(accent, panel) < 4.5; i++) {
         accent = blend(accent, 0xffffffff, 0.12);
      }
      return new StudioScreenTheme(0xee08090d, panel, row, hover, selected, accent);
   }

   static int blend(int base, int overlay, double weight) {
      int r = (int)Math.round(((base >>> 16) & 255) * (1 - weight) + ((overlay >>> 16) & 255) * weight);
      int g = (int)Math.round(((base >>> 8) & 255) * (1 - weight) + ((overlay >>> 8) & 255) * weight);
      int b = (int)Math.round((base & 255) * (1 - weight) + (overlay & 255) * weight);
      return 0xff000000 | r << 16 | g << 8 | b;
   }

   static double contrast(int a, int b) {
      double x = luminance(a), y = luminance(b);
      return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05);
   }

   private static double luminance(int argb) {
      return 0.2126 * linear((argb >>> 16) & 255) + 0.7152 * linear((argb >>> 8) & 255)
         + 0.0722 * linear(argb & 255);
   }

   private static double linear(int channel) {
      double v = channel / 255.0;
      return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
   }
}
