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

   /** Bounding rectangle of a valid group; null for empty or invalid geometry. */
   public static Bounds envelope(List<Bounds> bounds) {
      if(bounds.isEmpty()) return null;
      float left=Float.POSITIVE_INFINITY,top=Float.POSITIVE_INFINITY;
      float right=Float.NEGATIVE_INFINITY,bottom=Float.NEGATIVE_INFINITY;
      for(Bounds b:bounds) {
         if(!Float.isFinite(b.x())||!Float.isFinite(b.y())||!Float.isFinite(b.width())||!Float.isFinite(b.height())
            ||b.width()<0||b.height()<0||!Float.isFinite(b.x()+b.width())||!Float.isFinite(b.y()+b.height())) return null;
         left=Math.min(left,b.x());top=Math.min(top,b.y());
         right=Math.max(right,b.x()+b.width());bottom=Math.max(bottom,b.y()+b.height());
      }
      float w=right-left,h=bottom-top;
      return Float.isFinite(w)&&Float.isFinite(h)?new Bounds(left,top,w,h):null;
   }

   /** Validate one common displacement against every member before mutating any widget. */
   public static boolean fits(List<Bounds> bounds,float width,float height,Delta delta) {
      if(bounds.isEmpty()||!Float.isFinite(width)||!Float.isFinite(height)||width<=0||height<=0
         ||!Float.isFinite(delta.x())||!Float.isFinite(delta.y())) return false;
      for(Bounds b:bounds) {
         float x=b.x()+delta.x(),y=b.y()+delta.y();
         if(!Float.isFinite(x)||!Float.isFinite(y)||!Float.isFinite(b.width())||!Float.isFinite(b.height())
            ||b.width()<0||b.height()<0||x<0||y<0||(double)x+b.width()>width+0.001D||(double)y+b.height()>height+0.001D) return false;
      }
      return true;
   }
}
