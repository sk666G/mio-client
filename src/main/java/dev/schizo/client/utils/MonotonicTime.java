package dev.schizo.client.utils;

/** Shared subtraction-based elapsed-time checks for System.nanoTime() timestamps. */
public final class MonotonicTime {
   private MonotonicTime() {
   }

   public static boolean hasElapsed(long startNanos, long nowNanos, long durationNanos) {
      if (durationNanos <= 0L) {
         return true;
      }
      return nowNanos - startNanos >= durationNanos;
   }

   public static boolean isWithin(long startNanos, long nowNanos, long durationNanos) {
      return !hasElapsed(startNanos, nowNanos, durationNanos);
   }

   public static long elapsedNanos(long startNanos, long nowNanos) {
      long elapsed = nowNanos - startNanos;
      return elapsed < 0L ? 0L : elapsed;
   }
}
