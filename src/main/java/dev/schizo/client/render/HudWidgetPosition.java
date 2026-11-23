/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.render;

/** Shared viewport-safe positioning for HUD widgets. */
public final class HudWidgetPosition {
   private HudWidgetPosition() {
   }

   /**
    * Clamps an absolute widget coordinate so its extent remains inside the viewport.
    * Oversized widgets are pinned to the leading edge because no fully visible position exists.
    */
   public static float clampToViewport(float position, float viewportSize, float widgetSize) {
      if (!Float.isFinite(viewportSize) || viewportSize <= 0.0F) {
         return 0.0F;
      }

      float safeWidgetSize = Float.isFinite(widgetSize) ? Math.max(0.0F, widgetSize) : viewportSize;
      float maximumPosition = Math.max(0.0F, viewportSize - safeWidgetSize);
      if (Float.isNaN(position)) {
         return 0.0F;
      }

      return Math.max(0.0F, Math.min(position, maximumPosition));
   }

   /** Converts a viewport-clamped absolute coordinate to the renderer's normalized format. */
   public static float toNormalizedPosition(float position, float viewportSize, float widgetSize) {
      if (!Float.isFinite(viewportSize) || viewportSize <= 0.0F) {
         return 0.0F;
      }
      return clampToViewport(position, viewportSize, widgetSize) / viewportSize;
   }
}
