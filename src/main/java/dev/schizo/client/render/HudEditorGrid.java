package dev.schizo.client.render;

/** Optional HUD grid, independent from default anchor placement. Units are GUI-scaled pixels. */
public final class HudEditorGrid {
   private HudEditorGrid() {}

   public static int next(int step) {
      return switch (step) {
         case 0 -> 8;
         case 8 -> 16;
         case 16 -> 32;
         default -> 0;
      };
   }

   public static float snap(float position, float viewport, float extent, int step) {
      float clamped = HudWidgetPosition.clampToViewport(position, viewport, extent);
      if (step <= 0) return clamped;
      return HudWidgetPosition.clampToViewport((float)(Math.floor((double)clamped / step + 0.5) * step), viewport, extent);
   }

   /** Keep visual line count bounded even for very large viewports. */
   public static int visualStep(int step, int largestDimension) {
      if (step <= 0 || largestDimension <= 0) return 0;
      long multiple = Math.max(1L, ((long)largestDimension + 128L * step - 1L) / (128L * step));
      return (int)Math.min(Integer.MAX_VALUE, multiple * step);
   }
}
