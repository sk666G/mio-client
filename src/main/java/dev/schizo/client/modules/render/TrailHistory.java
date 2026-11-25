package dev.schizo.client.modules.render;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.function.Predicate;

/** Client-thread history. Evict the least recently sampled entity first. */
final class TrailHistory<T> {
   private final LinkedHashMap<Integer, ArrayDeque<T>> trails = new LinkedHashMap<>(16, 0.75F, true);
   private final int entityLimit;
   private final int pointLimit;

   TrailHistory(int entityLimit, int pointLimit) {
      if (entityLimit < 1 || pointLimit < 1) {
         throw new IllegalArgumentException("Trail limits must be positive");
      }
      this.entityLimit = entityLimit;
      this.pointLimit = pointLimit;
   }

   void add(int entityId, T point) {
      ArrayDeque<T> points = this.trails.get(entityId);
      if (points == null) {
         if (this.trails.size() == this.entityLimit) {
            this.trails.pollFirstEntry();
         }
         points = new ArrayDeque<>();
         this.trails.put(entityId, points);
      }
      if (points.size() == this.pointLimit) {
         points.removeFirst();
      }
      points.addLast(point);
   }

   void prune(Predicate<T> expired) {
      this.trails.values().removeIf(points -> {
         while (!points.isEmpty() && expired.test(points.getFirst())) {
            points.removeFirst();
         }
         return points.isEmpty();
      });
   }

   Collection<ArrayDeque<T>> values() {
      return this.trails.values();
   }

   void clear() {
      this.trails.clear();
   }
}
