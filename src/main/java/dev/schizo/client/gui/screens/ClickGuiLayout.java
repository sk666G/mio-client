package dev.schizo.client.gui.screens;

/** Pure layout math for keeping ClickGUI panels visible without rewriting the saved UI scale. */
final class ClickGuiLayout {
   private static final float MIN_EFFECTIVE_SCALE = 0.1F;

   private ClickGuiLayout() {
   }

   static float fitScale(
      float preferredScale,
      int viewportWidth,
      double baseMultiplier,
      int requiredWidth
   ) {
      if (preferredScale <= 0.0F || viewportWidth <= 0 || baseMultiplier <= 0.0D || requiredWidth <= 0) {
         return preferredScale;
      }

      double availableAtPreferred = viewportWidth / (baseMultiplier * preferredScale);
      if (availableAtPreferred >= requiredWidth) {
         return preferredScale;
      }

      float fitted = (float)(viewportWidth / (baseMultiplier * requiredWidth));
      return Math.max(MIN_EFFECTIVE_SCALE, Math.min(preferredScale, fitted));
   }
}
