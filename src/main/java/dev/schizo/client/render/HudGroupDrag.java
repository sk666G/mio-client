package dev.schizo.client.render;

import java.util.List;

/** Relative drag geometry. Grid mode snaps the shared displacement, never each member separately. */
public final class HudGroupDrag {
   private HudGroupDrag() {}

   /** Null indicates invalid geometry; caller should cancel the gesture. */
   public static HudGroupMove.Delta plan(List<HudGroupMove.Bounds> bounds, float width, float height,
                                         double dx, double dy, int gridStep) {
      if (bounds.size() < 2 || !Float.isFinite(width) || !Float.isFinite(height) || width <= 0 || height <= 0
         || !Double.isFinite(dx) || !Double.isFinite(dy)) return null;
      for (var b : bounds) {
         if (!Float.isFinite(b.x()) || !Float.isFinite(b.y()) || !Float.isFinite(b.width()) || !Float.isFinite(b.height())
            || b.width() < 0 || b.height() < 0 || b.x() < 0 || b.y() < 0
            || b.x() + b.width() > width + 0.001F || b.y() + b.height() > height + 0.001F) return null;
      }
      if (gridStep > 0) {
         dx = Math.floor(dx / gridStep + 0.5) * gridStep;
         dy = Math.floor(dy / gridStep + 0.5) * gridStep;
      }
      float x = (float)Math.max(-width, Math.min(width, dx));
      float y = (float)Math.max(-height, Math.min(height, dy));
      return HudGroupMove.clamp(bounds, width, height, x, y);
   }
}
