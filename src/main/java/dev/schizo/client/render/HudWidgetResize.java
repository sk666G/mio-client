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
         || contentHeight <= 0.0) {
         return fallback;
      }

      double scale = Math.min(
         (mouseX - widgetX) / contentWidth,
         (mouseY - widgetY) / contentHeight
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

   private static float clamp(float scale) {
      return Math.max(MIN_SCALE, Math.min(scale, MAX_SCALE));
   }
}
