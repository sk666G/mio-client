/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.render;

/** Pure pointer-to-widget coordinate conversion for HUD dragging. */
public final class HudWidgetDrag {
   private HudWidgetDrag() {
   }

   public static float clampCoordinate(
      double pointer,
      double grabOffset,
      float viewportSize,
      float widgetSize
   ) {
      if (!Double.isFinite(pointer) || !Double.isFinite(grabOffset)) {
         return 0.0F;
      }

      double position = pointer - grabOffset;
      if (!Double.isFinite(position)) {
         return 0.0F;
      }

      float finitePosition;
      if (position > Float.MAX_VALUE) {
         finitePosition = Float.MAX_VALUE;
      } else if (position < -Float.MAX_VALUE) {
         finitePosition = -Float.MAX_VALUE;
      } else {
         finitePosition = (float)position;
      }
      return HudWidgetPosition.clampToViewport(finitePosition, viewportSize, widgetSize);
   }
}
