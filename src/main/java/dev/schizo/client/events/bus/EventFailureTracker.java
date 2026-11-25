/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.events.bus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/** Bounded diagnostics for event listener failures without changing dispatch semantics. */
public final class EventFailureTracker {
   public static final int DEFAULT_CAPACITY = 64;
   public static final EventFailureTracker GLOBAL = new EventFailureTracker(DEFAULT_CAPACITY);

   private final int capacity;
   private final ConcurrentLinkedDeque<FailureSnapshot> recent = new ConcurrentLinkedDeque<>();
   private final AtomicLong totalFailures = new AtomicLong();

   public EventFailureTracker(int capacity) {
      if (capacity < 1) {
         throw new IllegalArgumentException("Event failure capacity must be positive");
      }
      this.capacity = capacity;
   }

   public void record(EventListener listener, Object event, Throwable failure) {
      Objects.requireNonNull(listener, "listener");
      Objects.requireNonNull(event, "event");
      Objects.requireNonNull(failure, "failure");

      this.totalFailures.incrementAndGet();
      this.recent.addFirst(new FailureSnapshot(
         listener.diagnosticName(),
         event.getClass().getSimpleName(),
         failure.getClass().getSimpleName(),
         truncate(failure.getMessage(), 240),
         Thread.currentThread().getName()
      ));
      while (this.recent.size() > this.capacity) {
         this.recent.pollLast();
      }
   }

   public long totalFailures() {
      return this.totalFailures.get();
   }

   public List<FailureSnapshot> recentFailures() {
      return List.copyOf(new ArrayList<>(this.recent));
   }

   public void reset() {
      this.totalFailures.set(0L);
      this.recent.clear();
   }

   private static String truncate(String value, int maximumLength) {
      if (value == null || value.length() <= maximumLength) {
         return value;
      }
      return value.substring(0, maximumLength);
   }

   public record FailureSnapshot(
      String listener,
      String eventType,
      String failureType,
      String message,
      String threadName
   ) {
   }
}
