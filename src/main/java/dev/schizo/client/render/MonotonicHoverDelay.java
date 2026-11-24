/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.render;

/** Monotonic one-shot hover delay that starts on the first active sample. */
final class MonotonicHoverDelay {
   private final long delayNanos;
   private long startedAtNanos;
   private boolean active;

   MonotonicHoverDelay(long delayNanos) {
      if (delayNanos < 0L) {
         throw new IllegalArgumentException("Hover delay cannot be negative");
      }
      this.delayNanos = delayNanos;
   }

   boolean hasElapsed(long nowNanos) {
      if (!this.active) {
         this.startedAtNanos = nowNanos;
         this.active = true;
         return this.delayNanos == 0L;
      }
      return nowNanos - this.startedAtNanos >= this.delayNanos;
   }

   void reset() {
      this.active = false;
   }
}
