package dev.schizo.client.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Plans selection-envelope alignment without mutating widgets or clipping the group. */
public final class HudGroupAlignment {
   public enum Mode { LEFT, RIGHT, TOP, BOTTOM, CENTER_X, CENTER_Y, DISTRIBUTE_X, DISTRIBUTE_Y }
   public record Position(float x, float y) {}

   private HudGroupAlignment() {}

   /** Empty result means the entire edit was rejected. Order matches the input bounds. */
   public static List<Position> plan(List<HudGroupMove.Bounds> bounds, float width, float height, Mode mode) {
      if (bounds.size() < 2 || mode == null || !Float.isFinite(width) || !Float.isFinite(height)
         || width <= 0 || height <= 0) return List.of();
      float left = width, right = 0, top = height, bottom = 0;
      for (var b : bounds) {
         if (!Float.isFinite(b.x()) || !Float.isFinite(b.y()) || !Float.isFinite(b.width()) || !Float.isFinite(b.height())
            || b.width() < 0 || b.height() < 0 || b.x() < 0 || b.y() < 0
            || b.x() + b.width() > width + 0.001F || b.y() + b.height() > height + 0.001F
            || b.width() > width || b.height() > height) return List.of();
         left = Math.min(left, b.x());
         top = Math.min(top, b.y());
         right = Math.max(right, b.x() + b.width());
         bottom = Math.max(bottom, b.y() + b.height());
      }
      if (mode == Mode.DISTRIBUTE_X || mode == Mode.DISTRIBUTE_Y) {
         return distribute(bounds, mode == Mode.DISTRIBUTE_X, width, height);
      }
      List<Position> positions = new ArrayList<>(bounds.size());
      for (var b : bounds) {
         float x = b.x(), y = b.y();
         switch (mode) {
            case LEFT -> x = left;
            case RIGHT -> x = right - b.width();
            case TOP -> y = top;
            case BOTTOM -> y = bottom - b.height();
            case CENTER_X -> x = left + (right - left - b.width()) / 2;
            case CENTER_Y -> y = top + (bottom - top - b.height()) / 2;
            case DISTRIBUTE_X, DISTRIBUTE_Y -> throw new IllegalStateException("Distribution is planned separately");
         }
         positions.add(new Position(HudWidgetPosition.clampToViewport(x, width, b.width()),
            HudWidgetPosition.clampToViewport(y, height, b.height())));
      }
      return List.copyOf(positions);
   }

   /** Equal edge-to-edge gaps, stable ordering by leading edge, fixed outer envelope.
    * Refuse insufficient space rather than introducing overlaps or silently expanding it.
    */
   private static List<Position> distribute(List<HudGroupMove.Bounds> bounds, boolean horizontal, float width, float height) {
      int n = bounds.size();
      if (n < 3) return List.of();
      List<Integer> order = new ArrayList<>(n);
      for (int i = 0; i < n; i++) order.add(i);
      order.sort(Comparator.comparingDouble(i -> leading(bounds.get(i), horizontal)));
      double start = leading(bounds.get(order.getFirst()), horizontal);
      double end = start;
      double totalExtent = 0;
      for (var b : bounds) {
         double extent = extent(b, horizontal);
         totalExtent += extent;
         end = Math.max(end, (double)leading(b, horizontal) + extent);
      }
      double remaining = end - start - totalExtent;
      if (remaining < -0.001) return List.of();
      double gap = Math.max(0, remaining) / (n - 1);
      Position[] positions = new Position[n];
      double cursor = start;
      for (int index : order) {
         var b = bounds.get(index);
         float x = horizontal ? (float)cursor : b.x();
         float y = horizontal ? b.y() : (float)cursor;
         positions[index] = new Position(HudWidgetPosition.clampToViewport(x, width, b.width()),
            HudWidgetPosition.clampToViewport(y, height, b.height()));
         cursor += extent(b, horizontal) + gap;
      }
      return List.of(positions);
   }

   private static float leading(HudGroupMove.Bounds b, boolean horizontal) {
      return horizontal ? b.x() : b.y();
   }

   private static float extent(HudGroupMove.Bounds b, boolean horizontal) {
      return horizontal ? b.width() : b.height();
   }
}
