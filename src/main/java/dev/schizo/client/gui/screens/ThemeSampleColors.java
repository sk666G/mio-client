package dev.schizo.client.gui.screens;

import dev.schizo.client.modules.client.UiPalette;

/** Opaque composited sample surfaces over a chosen reference backdrop, not live game rendering. */
record ThemeSampleColors(int surface, int nav, int button, int enabled, int accent, int navAccent,
                         int switchAccent, int gradientArgb) {
   private static final int TEXT = 0xffeeeeef, MUTED = 0xffb8b9c7;

   static ThemeSampleColors from(UiPalette palette, boolean light) {
      int surface = over(light ? 0xffc5c6cc : 0xff14151b, palette.background().getRGB());
      int nav = over(surface, palette.tint().getRGB());
      int button = over(surface, palette.button().getRGB());
      int enabled = over(surface, palette.enabled().getRGB());
      int accent = palette.accent().getRGB();
      return new ThemeSampleColors(surface, nav, button, enabled, over(surface,accent),
         over(nav,accent), over(enabled,accent), palette.gradient().getRGB());
   }

   static int over(int base, int foreground) {
      return StudioScreenTheme.blend(base, foreground, (foreground >>> 24) / 255.0);
   }

   int gradient(double fraction) {
      return StudioScreenTheme.blend(surface, gradientArgb,
         (gradientArgb >>> 24) / 255.0 * Math.max(0,Math.min(1,fraction)));
   }

   /** Minimum among the representative visible label pairs, including both switch states. */
   double minimumTextContrast() {
      return Math.min(Math.min(StudioScreenTheme.contrast(navAccent, nav), StudioScreenTheme.contrast(TEXT, nav)),
         Math.min(StudioScreenTheme.contrast(MUTED, button),
            Math.min(StudioScreenTheme.contrast(TEXT, button), StudioScreenTheme.contrast(TEXT, enabled))));
   }
}
