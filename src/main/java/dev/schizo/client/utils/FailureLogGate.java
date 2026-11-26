/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/** Lock-free time gate for noisy failure paths while retaining a suppressed-event count. */
public final class FailureLogGate {
   private final long intervalNanos;
   private final AtomicLong nextAllowedNanos = new AtomicLong();
   private final LongAdder suppressed = new LongAdder();

   public FailureLogGate(long intervalNanos) {
      if (intervalNanos <= 0L) {
         throw new IllegalArgumentException("Log interval must be positive");
      }
      this.intervalNanos = intervalNanos;
   }

   public Decision acquire(long nowNanos) {
      while (true) {
         long next = this.nextAllowedNanos.get();
         if (nowNanos < next) {
            this.suppressed.increment();
            return new Decision(false, 0L);
         }
         long replacement = saturatingAdd(nowNanos, this.intervalNanos);
         if (this.nextAllowedNanos.compareAndSet(next, replacement)) {
            long skipped = this.suppressed.sumThenReset();
            return new Decision(true, skipped);
         }
      }
   }

   private static long saturatingAdd(long left, long right) {
      long result = left + right;
      return result < left ? Long.MAX_VALUE : result;
   }

   public record Decision(boolean shouldLog, long suppressedSinceLastLog) {
   }
}
