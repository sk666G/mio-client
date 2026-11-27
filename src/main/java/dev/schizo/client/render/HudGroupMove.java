package dev.schizo.client.render;

import java.util.List;

/** Clamp a common group translation rather than distorting individual widget spacing. */
public final class HudGroupMove {
   private HudGroupMove() {}

   public record Bounds(float x, float y, float width, float height) {}
   public record Delta(float x, float y) {}

   public static Delta clamp(List<Bounds> bounds, float viewportWidth, float viewportHeight, float dx, float dy) {
      if (bounds.isEmpty() || !Float.isFinite(viewportWidth) || !Float.isFinite(viewportHeight)
         || viewportWidth <= 0 || viewportHeight <= 0 || !Float.isFinite(dx) || !Float.isFinite(dy)) return new Delta(0, 0);
      float minX = Float.NEGATIVE_INFINITY, minY = Float.NEGATIVE_INFINITY;
      float maxX = Float.POSITIVE_INFINITY, maxY = Float.POSITIVE_INFINITY;
      for (Bounds b : bounds) {
         if (!Float.isFinite(b.x()) || !Float.isFinite(b.y()) || !Float.isFinite(b.width()) || !Float.isFinite(b.height())
            || b.width() < 0 || b.height() < 0) return new Delta(0, 0);
         minX = Math.max(minX, -b.x());
         minY = Math.max(minY, -b.y());
         maxX = Math.min(maxX, viewportWidth - b.x() - b.width());
         maxY = Math.min(maxY, viewportHeight - b.y() - b.height());
      }
      return new Delta(axis(dx, minX, maxX), axis(dy, minY, maxY));
   }

   private static float axis(float requested, float min, float max) {
      if (min > max || requested == 0) return 0;
      float result = Math.max(min, Math.min(max, requested));
      return Math.signum(result) == Math.signum(requested) && Math.abs(result) <= Math.abs(requested) ? result : 0;
   }
}
