package dev.schizo.client.gui.screens;

/** Pure tooltip positioning math shared by client screens. */
final class ScreenTooltipLayout {
   private ScreenTooltipLayout() {
   }

   static float clampStart(float preferredStart, float extent, float viewportExtent) {
      if (!(extent > 0.0F) || !(viewportExtent > 0.0F)) {
         return preferredStart;
      }

      float maximumStart = Math.max(0.0F, viewportExtent - extent);
      return Math.max(0.0F, Math.min(preferredStart, maximumStart));
   }
}
