package dev.schizo.client.gui;

import dev.schizo.client.modules.client.UiPalette;
import java.awt.Color;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Versioned, data-only clipboard exchange. No names, settings, paths or executable content. */
public final class ThemeShareCode {
   private static final String PREFIX = "SCHIZO-THEME-1:";
   private static final int MAX_INPUT = 256;

   private ThemeShareCode() {}

   public static String encode(UiPalette palette) {
      Objects.requireNonNull(palette);
      return PREFIX + List.of(palette.accent(), palette.background(), palette.button(), palette.enabled(),
         palette.tint(), palette.gradient()).stream()
         .map(c -> PaletteHex.format(c).substring(1))
         .collect(Collectors.joining(":"));
   }

   public static UiPalette decode(String input) {
      if (input == null || input.length() > MAX_INPUT) throw new IllegalArgumentException("Invalid theme code length");
      String code = input.strip();
      if (!code.startsWith(PREFIX)) throw new IllegalArgumentException("Unsupported theme code");
      String[] channels = code.substring(PREFIX.length()).split(":", -1);
      if (channels.length != 6) throw new IllegalArgumentException("Theme requires six RGBA colors");
      Color[] colors = new Color[6];
      for (int i = 0; i < colors.length; i++) {
         if (!channels[i].matches("[0-9a-fA-F]{8}")) throw new IllegalArgumentException("Invalid RGBA color");
         colors[i] = PaletteHex.parse(channels[i]);
      }
      return new UiPalette(colors[0],colors[1],colors[2],colors[3],colors[4],colors[5]);
   }
}
