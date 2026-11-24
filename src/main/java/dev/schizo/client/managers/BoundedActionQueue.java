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
import java.util.Objects;
import java.util.function.Consumer;

/** Bounded, thread-safe FIFO queue for deferred client-thread operations. */
final class BoundedActionQueue {
   private final int capacity;
   private final Object lock = new Object();
   private final ArrayDeque<Runnable> actions = new ArrayDeque<>();
   /** Guarded by {@link #lock}; actions execute outside the monitor. */
   private boolean draining;

   BoundedActionQueue(int capacity) {
      if (capacity < 1) {
         throw new IllegalArgumentException("Queue capacity must be positive");
      }
      this.capacity = capacity;
   }

   boolean offer(Runnable action) {
      Objects.requireNonNull(action, "action");
      synchronized (this.lock) {
         if (this.actions.size() >= this.capacity) {
            return false;
         }
         this.actions.addLast(action);
         return true;
      }
   }

   int drain(int maximumActions, Consumer<Runnable> executor) {
      Objects.requireNonNull(executor, "executor");
      if (maximumActions < 1) {
         return 0;
      }

      synchronized (this.lock) {
         if (this.draining) {
            return 0;
         }
         this.draining = true;
      }

      int executed = 0;
      try {
         while (executed < maximumActions) {
            Runnable action;
            synchronized (this.lock) {
               action = this.actions.pollFirst();
            }
            if (action == null) {
               break;
            }
            executor.accept(action);
            executed++;
         }
         return executed;
      } finally {
         synchronized (this.lock) {
            this.draining = false;
         }
      }
   }

   int size() {
      synchronized (this.lock) {
         return this.actions.size();
      }
   }

   boolean isFull() {
      synchronized (this.lock) {
         return this.actions.size() >= this.capacity;
      }
   }

   boolean isEmpty() {
      synchronized (this.lock) {
         return this.actions.isEmpty();
      }
   }
}
