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
import java.util.Queue;
import java.util.function.Consumer;

/** Bounded FIFO queue for deferred client-thread operations. */
final class BoundedActionQueue {
   private final int capacity;
   private final Queue<Runnable> actions = new ArrayDeque<>();
   private boolean draining;

   BoundedActionQueue(int capacity) {
      if (capacity < 1) {
         throw new IllegalArgumentException("Queue capacity must be positive");
      }
      this.capacity = capacity;
   }

   boolean offer(Runnable action) {
      Objects.requireNonNull(action, "action");
      if (this.actions.size() >= this.capacity) {
         return false;
      }
      return this.actions.offer(action);
   }

   int drain(int maximumActions, Consumer<Runnable> executor) {
      Objects.requireNonNull(executor, "executor");
      if (maximumActions < 1 || this.draining) {
         return 0;
      }

      this.draining = true;
      int executed = 0;
      try {
         while (executed < maximumActions) {
            Runnable action = this.actions.poll();
            if (action == null) {
               break;
            }
            executor.accept(action);
            executed++;
         }
         return executed;
      } finally {
         this.draining = false;
      }
   }

   int size() {
      return this.actions.size();
   }

   boolean isFull() {
      return this.actions.size() >= this.capacity;
   }

   boolean isEmpty() {
      return this.actions.isEmpty();
   }
}
