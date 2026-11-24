/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.render;

/** Pure HUD axis snapping shared by drag updates and guide rendering. */
public final class HudWidgetSnap {
   public enum Target {
      NONE,
      CENTER,
      LEADING,
      TRAILING
   }

   private HudWidgetSnap() {
   }

   public static Target resolve(
      float position, float widgetSize, float screenSize, float margin, float threshold
   ) {
      if (!Float.isFinite(position) || !Float.isFinite(widgetSize) || !Float.isFinite(screenSize)
         || !Float.isFinite(margin) || !Float.isFinite(threshold) || widgetSize < 0.0F
         || screenSize <= 0.0F || threshold < 0.0F) {
         return Target.NONE;
      }

      float center = screenSize / 2.0F;
      if (Math.abs(position + widgetSize / 2.0F - center) <= threshold) {
         return Target.CENTER;
      }
      if (Math.abs(position - margin) <= threshold) {
         return Target.LEADING;
      }
      if (Math.abs(position + widgetSize - (screenSize - margin)) <= threshold) {
         return Target.TRAILING;
      }
      return Target.NONE;
   }

   public static float position(Target target, float widgetSize, float screenSize, float margin) {
      return switch (target) {
         case CENTER -> screenSize / 2.0F - widgetSize / 2.0F;
         case LEADING -> margin;
         case TRAILING -> screenSize - margin - widgetSize;
         case NONE -> Float.NaN;
      };
   }

   public static float snap(
      float position, float widgetSize, float screenSize, float margin, float threshold
   ) {
      Target target = resolve(position, widgetSize, screenSize, margin, threshold);
      return target == Target.NONE ? position : position(target, widgetSize, screenSize, margin);
   }
}
