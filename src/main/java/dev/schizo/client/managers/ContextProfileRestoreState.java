/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** Transactional baseline capture and retryable per-key restoration bookkeeping. */
final class ContextProfileRestoreState {
   private ContextProfileRestoreState() {}

   static <K, V> boolean capture(
      Map<K, V> baseline,
      Set<K> modified,
      Iterable<K> orderedKeys,
      Function<K, V> snapshotter,
      BiConsumer<K, RuntimeException> failureHandler
   ) {
      if (!modified.isEmpty()) {
         return false;
      }

      Map<K, V> snapshots = new LinkedHashMap<>();
      for (K key : orderedKeys) {
         try {
            V snapshot = snapshotter.apply(key);
            if (snapshot == null) {
               throw new IllegalStateException("Snapshot provider returned null");
            }
            snapshots.put(key, snapshot);
         } catch (RuntimeException failure) {
            failureHandler.accept(key, failure);
            return false;
         }
      }

      baseline.clear();
      baseline.putAll(snapshots);
      return true;
   }

   static <K, V> boolean restore(
      Map<K, V> baseline,
      Set<K> modified,
      Iterable<K> orderedKeys,
      BiConsumer<K, V> restorer,
      BiConsumer<K, RuntimeException> failureHandler
   ) {
      boolean restored = true;
      for (K key : orderedKeys) {
         if (!modified.contains(key)) {
            continue;
         }

         V snapshot = baseline.get(key);
         if (snapshot == null) {
            failureHandler.accept(key, new IllegalStateException("No baseline snapshot exists"));
            restored = false;
            continue;
         }

         try {
            restorer.accept(key, snapshot);
            modified.remove(key);
         } catch (RuntimeException failure) {
            failureHandler.accept(key, failure);
            restored = false;
         }
      }
      return restored && modified.isEmpty();
   }
}
