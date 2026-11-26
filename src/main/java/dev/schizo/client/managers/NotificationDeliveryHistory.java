/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import java.util.ArrayDeque;
import java.util.List;

/** Bounded, payload-free history of notification delivery outcomes. */
public final class NotificationDeliveryHistory {
   public static final int DEFAULT_CAPACITY = 20;

   public enum Status {
      DELIVERED,
      FAILED,
      DROPPED
   }

   public record Entry(long timestampMillis, Status status, int messageCount, String detail) {
      public Entry {
         if (status == null) {
            throw new IllegalArgumentException("Delivery status is required");
         }
         if (messageCount < 0) {
            throw new IllegalArgumentException("Message count cannot be negative");
         }
         detail = detail == null ? "" : detail;
         if (detail.length() > 96) {
            detail = detail.substring(0, 96);
         }
      }
   }

   private final int capacity;
   private final ArrayDeque<Entry> entries;

   public NotificationDeliveryHistory() {
      this(DEFAULT_CAPACITY);
   }

   public NotificationDeliveryHistory(int capacity) {
      if (capacity < 1 || capacity > 256) {
         throw new IllegalArgumentException("Delivery history capacity must be between 1 and 256");
      }
      this.capacity = capacity;
      this.entries = new ArrayDeque<>(capacity);
   }

   public synchronized void record(Entry entry) {
      if (entry == null) {
         throw new IllegalArgumentException("Delivery history entry is required");
      }
      while (this.entries.size() >= this.capacity) {
         this.entries.removeFirst();
      }
      this.entries.addLast(entry);
   }

   public synchronized List<Entry> snapshot() {
      return List.copyOf(this.entries);
   }

   public synchronized int size() {
      return this.entries.size();
   }

   public synchronized void clear() {
      this.entries.clear();
   }
}
