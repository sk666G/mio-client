/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils;

/**
 * Rate-limits refresh work using a monotonic nanosecond timestamp.
 *
 * The subtraction-based elapsed-time check remains valid across nanoTime's
 * signed-long wraparound for intervals shorter than half the counter range.
 */
public final class MonotonicRefreshGate {
   private final long intervalNanos;
   private long lastRefreshNanos;
   private boolean initialized;

   public MonotonicRefreshGate(long intervalNanos) {
      if (intervalNanos <= 0L) {
         throw new IllegalArgumentException("Refresh interval must be positive");
      }
      this.intervalNanos = intervalNanos;
   }

   /**
    * Returns true when a refresh is due and records the refresh timestamp.
    * A forced refresh also resets the cadence for the next ordinary refresh.
    */
   public boolean shouldRefresh(long nowNanos, boolean force) {
      if (!this.initialized || force || nowNanos - this.lastRefreshNanos >= this.intervalNanos) {
         this.lastRefreshNanos = nowNanos;
         this.initialized = true;
         return true;
      }
      return false;
   }
}
