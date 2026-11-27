package dev.schizo.client.gui;

import java.awt.Color;
import java.util.Locale;

/** Strict fixed-color exchange format: #RRGGBBAA or #RRGGBB (opaque). */
public final class PaletteHex {
   private PaletteHex() {}

   public static Color parse(String text) {
      if (text == null) throw new IllegalArgumentException("Missing color");
      String raw = text.strip();
      if (raw.startsWith("#")) raw = raw.substring(1);
      if (!raw.matches("[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")) throw new IllegalArgumentException("Use RRGGBB or RRGGBBAA");
      long bits = Long.parseLong(raw, 16);
      if (raw.length() == 6) return new Color((int)bits);
      return new Color((int)(bits >>> 24), (int)(bits >>> 16) & 255, (int)(bits >>> 8) & 255, (int)bits & 255);
   }

   public static String format(Color color) {
      return String.format(Locale.ROOT, "#%02X%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
   }
}
