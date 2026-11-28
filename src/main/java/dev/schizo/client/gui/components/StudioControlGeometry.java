package dev.schizo.client.gui.components;

/** Geometry shared by the docked controls; independent of rendering and input state. */
final class StudioControlGeometry {
   private StudioControlGeometry() {}

   static float fraction(double value, double min, double max) {
      double span = max - min;
      if (!Double.isFinite(value) || !Double.isFinite(span) || span <= 0) return 0.0F;
      return (float)Math.max(0.0, Math.min(1.0, (value - min) / span));
   }

   static float clampProgress(float value) {
      return Float.isFinite(value) ? Math.max(0.0F, Math.min(1.0F, value)) : 0.0F;
   }

   static float thumbLeft(float left, float width, float diameter, float progress) {
      return left + Math.max(0.0F, width - diameter) * clampProgress(progress);
   }
}
