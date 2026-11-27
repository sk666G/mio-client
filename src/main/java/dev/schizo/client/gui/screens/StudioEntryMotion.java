package dev.schizo.client.gui.screens;

/** Bounded, monotonic entry veil; no position transforms or input delays. */
final class StudioEntryMotion {
   static final long DURATION_NANOS = 180_000_000L;
   static final int MAX_ALPHA = 64;

   private StudioEntryMotion() {}

   static int veilAlpha(long elapsedNanos) {
      if (elapsedNanos <= 0) return MAX_ALPHA;
      if (elapsedNanos >= DURATION_NANOS) return 0;
      double remaining = 1.0 - (double)elapsedNanos / DURATION_NANOS;
      return (int)Math.round(MAX_ALPHA * remaining * remaining * remaining);
   }
}
