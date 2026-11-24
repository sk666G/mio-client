package dev.schizo.client.gui.screens;

final class PerformanceViewport {
   private PerformanceViewport() {
   }

   static float fitScale(float width, float height) {
      if (width <= 0.0F || height <= 0.0F) {
         return 1.0F;
      }
      // Keep the dashboard's 280 x 320 minimum layout and ten-pixel margins visible.
      return Math.min(1.0F, Math.min(width / 300.0F, height / 340.0F));
   }
}
