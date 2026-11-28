package dev.schizo.client.gui.screens;

import dev.schizo.client.modules.client.UIModule;
import dev.schizo.client.modules.client.UiPalette;

/** Shared dark workspace chrome. Text accents are adjusted for contrast, not written back to settings. */
record StudioScreenTheme(int backdrop, int panel, int row, int hover, int selected, int accent) {
   private static final Cache CACHE = new Cache();

   /** Only three RGB values affect opaque Studio chrome; alpha and other palette slots do not. */
   static final class Cache {
      private int accent, background, button;
      private StudioScreenTheme theme;

      StudioScreenTheme resolve(int accent,int background,int button) {
         accent|=0xff000000;
         background|=0xff000000;
         button|=0xff000000;
         if(theme==null || this.accent!=accent || this.background!=background || this.button!=button) {
            theme=fromColors(accent,background,button);
            this.accent=accent;this.background=background;this.button=button;
         }
         return theme;
      }
   }

   // Sample the live colors on every call, including rainbow and synced colors.
   // Avoid creating a six-color UiPalette snapshot for every rendered widget.
   static StudioScreenTheme current() {
      UIModule ui=UIModule.INSTANCE;
      return CACHE.resolve(ui.colorSetting.getValue().getRGB(),ui.backgroundSetting.getValue().getRGB(),ui.buttonSetting.getValue().getRGB());
   }

   static StudioScreenTheme from(UiPalette palette) {
      return fromColors(palette.accent().getRGB(),palette.background().getRGB(),palette.button().getRGB());
   }

   private static StudioScreenTheme fromColors(int accentRgb,int backgroundRgb,int buttonRgb) {
      int panel=blend(0xff13141a,backgroundRgb,0.32);
      int row=blend(0xff21222a,buttonRgb,0.22);
      int hover=blend(row,0xffeeeeef,0.08);
      int selected=blend(row,accentRgb,0.16);
      int accent=accentRgb | 0xff000000;
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
