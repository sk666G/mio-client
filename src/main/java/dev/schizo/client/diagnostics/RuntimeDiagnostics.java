/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Schizo runtime diagnostics.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */
package dev.schizo.client.diagnostics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bounded process-local diagnostics for recoverable runtime failures.
 *
 * <p>The registry deliberately stores only compact immutable descriptions rather than Throwable
 * instances so failures cannot retain large object graphs for the rest of the session.</p>
 */
public final class RuntimeDiagnostics {
   public static final int MAX_RECENT_FAILURES = 128;
   private static final Logger LOGGER = LoggerFactory.getLogger("Schizo");
   private static final Object RECENT_LOCK = new Object();
   private static final ArrayDeque<FailureRecord> RECENT = new ArrayDeque<>();
   private static final ConcurrentHashMap<String, LongAdder> COUNTS = new ConcurrentHashMap<>();
   private static final LongAdder TOTAL = new LongAdder();
   private static final AtomicLong SEQUENCE = new AtomicLong();

   private RuntimeDiagnostics() {
   }

   public static void record(Area area, String owner, Throwable failure) {
      Objects.requireNonNull(area, "area");
      Objects.requireNonNull(failure, "failure");
      String normalizedOwner = owner == null || owner.isBlank() ? "unknown" : owner;
      String failureType = failure.getClass().getName();
      String message = failure.getMessage();
      if (message == null || message.isBlank()) {
         message = failureType;
      } else if (message.length() > 512) {
         message = message.substring(0, 512);
      }

      String key = area.name() + "|" + normalizedOwner + "|" + failureType;
      LongAdder count = COUNTS.computeIfAbsent(key, ignored -> new LongAdder());
      count.increment();
      long occurrence = count.sum();
      TOTAL.increment();

      FailureRecord record = new FailureRecord(
         SEQUENCE.incrementAndGet(),
         System.currentTimeMillis(),
         area,
         normalizedOwner,
         Thread.currentThread().getName(),
         failureType,
         message,
         occurrence
      );
      synchronized (RECENT_LOCK) {
         if (RECENT.size() == MAX_RECENT_FAILURES) {
            RECENT.removeFirst();
         }
         RECENT.addLast(record);
      }

      // Keep logs useful during repeated failures without turning one broken listener into a log flood.
      if (occurrence <= 3L || occurrence % 100L == 0L) {
         LOGGER.error(
            "Recoverable {} failure in {} on thread {} (occurrence {})",
            area.name().toLowerCase(java.util.Locale.ROOT),
            normalizedOwner,
            record.threadName(),
            occurrence,
            failure
         );
      }
   }

   public static long totalFailureCount() {
      return TOTAL.sum();
   }

   public static List<FailureRecord> recentFailures() {
      synchronized (RECENT_LOCK) {
         return List.copyOf(new ArrayList<>(RECENT));
      }
   }

   public static void clear() {
      synchronized (RECENT_LOCK) {
         RECENT.clear();
      }
      COUNTS.clear();
      TOTAL.reset();
      SEQUENCE.set(0L);
   }

   public enum Area {
      EVENT,
      MODULE_LIFECYCLE,
      SETTING_CALLBACK,
      CONFIG_SERIALIZATION,
      SCHEDULED_TASK,
      GUI,
      INTEGRATION,
      MIXIN,
      COMMAND,
      RENDER,
      FILE_IO
   }

   public record FailureRecord(
      long sequence,
      long timestampMillis,
      Area area,
      String owner,
      String threadName,
      String failureType,
      String message,
      long occurrence
   ) {
   }
}
