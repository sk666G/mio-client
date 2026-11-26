package dev.schizo.client.gui.screens;

import java.util.Locale;

/** A profiler snapshot row with lazily cached display text, owned by the GUI thread. */
final class ProfilerDisplayRow {
   private final String name;
   private final long calls;
   private final long totalNanos;
   private final long averageNanos;
   private final long maximumNanos;
   private Values values;
   private int cachedNameLimit;
   private String cachedName;

   ProfilerDisplayRow(String name, long calls, long totalNanos, long averageNanos, long maximumNanos) {
      this.name = name;
      this.calls = calls;
      this.totalNanos = totalNanos;
      this.averageNanos = averageNanos;
      this.maximumNanos = maximumNanos;
   }

   Values values() {
      if (this.values == null) {
         this.values = new Values(
            Long.toString(this.calls),
            milliseconds(this.averageNanos),
            milliseconds(this.maximumNanos),
            milliseconds(this.totalNanos)
         );
      }
      return this.values;
   }

   String displayName(int maximumLength) {
      int limit = Math.max(1, maximumLength);
      if (this.cachedName == null || this.cachedNameLimit != limit) {
         this.cachedNameLimit = limit;
         this.cachedName = this.name.length() <= limit
            ? this.name
            : this.name.substring(0, limit - 1) + "…";
      }
      return this.cachedName;
   }

   private static String milliseconds(long nanos) {
      return String.format(Locale.ROOT, "%.3f", nanos / 1_000_000.0D);
   }

   record Values(String calls, String average, String maximum, String total) { }
}
