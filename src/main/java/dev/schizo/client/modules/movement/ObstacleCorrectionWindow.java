/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.movement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class ObstacleCorrectionWindow {
   private final List<Long> corrections;
   private final long windowNanos;
   private final int maximumCorrections;

   public void clear() {
      this.corrections.clear();
   }

   public void pruneExpired() {
      long nowNanos = System.nanoTime();
      this.corrections.removeIf(timestamp -> nowNanos - timestamp > this.windowNanos);
   }

   public ObstacleCorrectionWindow(int maximumCorrections) {
      this(1000, maximumCorrections);
   }

   public ObstacleCorrectionWindow(int windowMillis, int maximumCorrections) {
      this.corrections = Collections.synchronizedList(new ArrayList<>());
      this.windowNanos = TimeUnit.MILLISECONDS.toNanos(windowMillis);
      this.maximumCorrections = maximumCorrections;
   }

   public void recordCorrection() {
      this.corrections.add(System.nanoTime());
   }

   public boolean exceedsLimit() {
      return this.corrections.size() > this.maximumCorrections;
   }
}
