/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.profiling;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * Lightweight process-local failure counters for operational diagnostics.
 *
 * This deliberately stores only the latest failure summary, not stack traces or
 * unbounded history. Full stack traces remain in the normal logger.
 */
public final class RuntimeDiagnostics {
   public static final RuntimeDiagnostics GLOBAL = new RuntimeDiagnostics();

   private final EnumMap<Area, LongAdder> counts = new EnumMap<>(Area.class);
   private final AtomicLong totalFailures = new AtomicLong();
   private final AtomicReference<FailureSnapshot> lastFailure = new AtomicReference<>();

   public RuntimeDiagnostics() {
      for (Area area : Area.values()) {
         this.counts.put(area, new LongAdder());
      }
   }

   public void record(Area area, String detail, Throwable failure) {
      Objects.requireNonNull(area, "area");
      Objects.requireNonNull(failure, "failure");

      this.counts.get(area).increment();
      long total = this.totalFailures.incrementAndGet();
      this.lastFailure.set(new FailureSnapshot(
         total,
         area,
         sanitize(detail, 160),
         failure.getClass().getSimpleName(),
         sanitize(failure.getMessage(), 240),
         System.nanoTime()
      ));
   }

   public Snapshot snapshot() {
      EnumMap<Area, Long> currentCounts = new EnumMap<>(Area.class);
      for (Area area : Area.values()) {
         currentCounts.put(area, this.counts.get(area).sum());
      }
      return new Snapshot(
         this.totalFailures.get(),
         Map.copyOf(currentCounts),
         this.lastFailure.get()
      );
   }

   public void reset() {
      for (LongAdder counter : this.counts.values()) {
         counter.reset();
      }
      this.totalFailures.set(0L);
      this.lastFailure.set(null);
   }

   private static String sanitize(String text, int maximumLength) {
      if (text == null || text.isBlank()) {
         return "";
      }
      String normalized = text.replace('\n', ' ').replace('\r', ' ').strip();
      return normalized.length() <= maximumLength
         ? normalized
         : normalized.substring(0, maximumLength);
   }

   public enum Area {
      MODULE_LIFECYCLE,
      EVENT_DISPATCH,
      SETTING_CALLBACK,
      CONFIG_SERIALIZATION,
      SCHEDULED_TASK,
      INTEGRATION
   }

   public record Snapshot(
      long totalFailures,
      Map<Area, Long> counts,
      FailureSnapshot lastFailure
   ) {}

   public record FailureSnapshot(
      long sequence,
      Area area,
      String detail,
      String failureType,
      String message,
      long recordedAtNanos
   ) {}
}
