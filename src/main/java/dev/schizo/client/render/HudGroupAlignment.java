package dev.schizo.client.render;

import java.util.ArrayList;
import java.util.List;

/** Plans selection-envelope alignment without mutating widgets or clipping the group. */
public final class HudGroupAlignment {
   public enum Mode { LEFT, RIGHT, TOP, BOTTOM, CENTER_X, CENTER_Y }
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
         }
         positions.add(new Position(HudWidgetPosition.clampToViewport(x, width, b.width()),
            HudWidgetPosition.clampToViewport(y, height, b.height())));
      }
      return List.copyOf(positions);
   }
}
