package dev.schizo.client.utils;

/** Pure monotonic countdown math for UI and reconnect timers. */
public final class MonotonicCountdown {
   private static final double NANOS_PER_SECOND = 1_000_000_000.0D;

   private MonotonicCountdown() {
   }

   public static float remainingSeconds(long startNanos, long nowNanos, float durationSeconds) {
      if (!(durationSeconds > 0.0F)) {
         return 0.0F;
      }

      long elapsedNanos = nowNanos - startNanos;
      if (elapsedNanos <= 0L) {
         return durationSeconds;
      }

      double elapsedSeconds = elapsedNanos / NANOS_PER_SECOND;
      return (float)Math.max(durationSeconds - elapsedSeconds, 0.0D);
   }
}
