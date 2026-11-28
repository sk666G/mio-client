/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.render;

/** Pure, guarded resize-scale calculation for HUD widgets. */
public final class HudWidgetResize {
   private static final float MIN_SCALE = 0.5F;
   private static final float MAX_SCALE = 2.0F;

   private HudWidgetResize() {
   }

   /**
    * Calculates a resize scale from the pointer position. Invalid or degenerate inputs leave
    * the current scale unchanged rather than allowing NaN or infinity into a numeric setting.
    */
   public static float calculateScale(
      double mouseX,
      double mouseY,
      double widgetX,
      double widgetY,
      double contentWidth,
      double contentHeight,
      float currentScale
   ) {
      return calculateScale(mouseX, mouseY, widgetX, widgetY, contentWidth, contentHeight, currentScale, 1.0);
   }

   /**
    * Converts pointer coordinates into the widget's own size setting, excluding the
    * additional global HUD scale applied during rendering.
    */
   public static float calculateScale(
      double mouseX,
      double mouseY,
      double widgetX,
      double widgetY,
      double contentWidth,
      double contentHeight,
      float currentScale,
      double hudScaleMultiplier
   ) {
      float fallback = Float.isFinite(currentScale)
         ? clamp(currentScale)
         : 1.0F;
      if (!Double.isFinite(mouseX)
         || !Double.isFinite(mouseY)
         || !Double.isFinite(widgetX)
         || !Double.isFinite(widgetY)
         || !Double.isFinite(contentWidth)
         || !Double.isFinite(contentHeight)
         || contentWidth <= 0.0
         || contentHeight <= 0.0
         || !Double.isFinite(hudScaleMultiplier)
         || hudScaleMultiplier <= 0.0) {
         return fallback;
      }

      double effectiveWidth = contentWidth * hudScaleMultiplier;
      double effectiveHeight = contentHeight * hudScaleMultiplier;
      if (!Double.isFinite(effectiveWidth) || !Double.isFinite(effectiveHeight)
         || effectiveWidth <= 0.0 || effectiveHeight <= 0.0) {
         return fallback;
      }

      double scale = Math.min(
         (mouseX - widgetX) / effectiveWidth,
         (mouseY - widgetY) / effectiveHeight
      );
      if (!Double.isFinite(scale)) {
         return fallback;
      }

      double rounded = Math.floor(scale * 100.0) / 100.0;
      if (!Double.isFinite(rounded)) {
         return fallback;
      }
      return clamp((float)rounded);
   }

   /** Adjust the widget's own scale in whole percentage points, independent of global HUD scale. */
   public static float adjustPercent(float currentScale, int points) {
      if (!Float.isFinite(currentScale)) return 1.0F;
      long percent = Math.round(clamp(currentScale) * 100.0F);
      return Math.max(50L, Math.min(200L, percent + (long)points)) / 100.0F;
   }

   private static float clamp(float scale) {
      return Math.max(MIN_SCALE, Math.min(scale, MAX_SCALE));
   }
}
