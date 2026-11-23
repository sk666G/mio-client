package dev.schizo.client.managers;

import java.util.Objects;
import java.util.function.LongSupplier;

/** Small retry gate for Context Profile rollback after a partial category restore failure. */
final class ContextProfileRestoreRetryGate {
   private final LongSupplier clock;
   private final long retryDelayMillis;
   private long nextAttemptMillis;

   ContextProfileRestoreRetryGate(LongSupplier clock, long retryDelayMillis) {
      this.clock = Objects.requireNonNull(clock, "clock");
      if (retryDelayMillis <= 0L) {
         throw new IllegalArgumentException("Retry delay must be positive");
      }
      this.retryDelayMillis = retryDelayMillis;
   }

   boolean isReady() {
      return this.clock.getAsLong() >= this.nextAttemptMillis;
   }

   void recordFailure() {
      long now = this.clock.getAsLong();
      this.nextAttemptMillis = now > Long.MAX_VALUE - this.retryDelayMillis
         ? Long.MAX_VALUE
         : now + this.retryDelayMillis;
   }

   void recordSuccess() {
      this.nextAttemptMillis = 0L;
   }
}
