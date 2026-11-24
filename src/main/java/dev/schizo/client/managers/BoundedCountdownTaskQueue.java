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
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Bounded FIFO queue for countdown tasks. Reservations count toward capacity before a task is
 * dispatched to the client thread, so the dispatch hand-off cannot create an unbounded backlog.
 */
final class BoundedCountdownTaskQueue {
   static final int DEFAULT_CAPACITY = 4096;
   static final int DEFAULT_MAX_TASKS_PER_PASS = 256;

   private final Object lock = new Object();
   private final ArrayDeque<Reservation> queue = new ArrayDeque<>();
   private final Set<Reservation> reservations = Collections.newSetFromMap(new IdentityHashMap<>());
   private final int capacity;
   private final int maximumTasksPerPass;
   private int outstanding;
   private long passSequence;
   private boolean draining;
   private boolean closed;

   BoundedCountdownTaskQueue(int capacity, int maximumTasksPerPass) {
      if (capacity < 1) {
         throw new IllegalArgumentException("Queue capacity must be positive");
      }
      if (maximumTasksPerPass < 1) {
         throw new IllegalArgumentException("Maximum tasks per pass must be positive");
      }
      this.capacity = capacity;
      this.maximumTasksPerPass = maximumTasksPerPass;
   }

   Reservation reserve(Runnable action, int delayPasses) {
      Objects.requireNonNull(action, "action");
      if (delayPasses < 0) {
         throw new IllegalArgumentException("Task delay must not be negative");
      }
      synchronized (this.lock) {
         if (this.closed || this.outstanding >= this.capacity) {
            return null;
         }
         Reservation reservation = new Reservation(this, action, delayPasses);
         this.reservations.add(reservation);
         this.outstanding++;
         return reservation;
      }
   }

   boolean enqueue(Reservation reservation) {
      this.requireOwner(reservation);
      synchronized (this.lock) {
         if (reservation.state != ReservationState.RESERVED) {
            return false;
         }
         if (this.closed) {
            this.cancelLocked(reservation);
            return false;
         }
         reservation.readyAtPass = this.passSequence + reservation.delayPasses + 2L;
         reservation.state = ReservationState.QUEUED;
         this.queue.addLast(reservation);
         return true;
      }
   }

   boolean cancel(Reservation reservation) {
      this.requireOwner(reservation);
      synchronized (this.lock) {
         if (reservation.state != ReservationState.RESERVED) {
            return false;
         }
         this.cancelLocked(reservation);
         return true;
      }
   }

   boolean offer(Runnable action, int delayPasses) {
      Reservation reservation = this.reserve(action, delayPasses);
      return reservation != null && this.enqueue(reservation);
   }

   /**
    * Drains ready tasks in strict FIFO order. A not-yet-ready head deliberately blocks later
    * entries, and tasks enqueued by a running action cannot execute in the same pass.
    */
   int runPass(Consumer<RuntimeException> failureHandler) {
      Objects.requireNonNull(failureHandler, "failureHandler");
      synchronized (this.lock) {
         if (this.closed || this.draining) {
            return 0;
         }
         this.passSequence++;
         if (this.queue.isEmpty()) {
            return 0;
         }
         this.draining = true;
      }

      int completed = 0;
      try {
         while (completed < this.maximumTasksPerPass) {
            Reservation reservation;
            synchronized (this.lock) {
               reservation = this.queue.peekFirst();
               if (reservation == null || reservation.readyAtPass > this.passSequence) {
                  break;
               }
               this.queue.removeFirst();
               if (reservation.state != ReservationState.QUEUED) {
                  continue;
               }
               reservation.state = ReservationState.RUNNING;
            }

            try {
               reservation.action.run();
            } catch (RuntimeException failure) {
               failureHandler.accept(failure);
            } finally {
               this.complete(reservation);
            }
            completed++;
         }
         return completed;
      } finally {
         synchronized (this.lock) {
            this.draining = false;
         }
      }
   }

   int clear() {
      synchronized (this.lock) {
         this.queue.clear();
         int cancelled = 0;
         var iterator = this.reservations.iterator();
         while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            if (reservation.state == ReservationState.RUNNING) {
               continue;
            }
            reservation.state = ReservationState.CANCELLED;
            iterator.remove();
            this.outstanding--;
            cancelled++;
         }
         return cancelled;
      }
   }

   void close() {
      synchronized (this.lock) {
         if (this.closed) {
            return;
         }
         this.closed = true;
         this.queue.clear();
         for (Reservation reservation : this.reservations) {
            reservation.state = ReservationState.CANCELLED;
         }
         this.reservations.clear();
         this.outstanding = 0;
      }
   }

   int queuedCount() {
      synchronized (this.lock) {
         return this.queue.size();
      }
   }

   int outstandingCount() {
      synchronized (this.lock) {
         return this.outstanding;
      }
   }

   boolean isClosed() {
      synchronized (this.lock) {
         return this.closed;
      }
   }

   private void complete(Reservation reservation) {
      synchronized (this.lock) {
         if (reservation.state != ReservationState.RUNNING) {
            return;
         }
         reservation.state = ReservationState.COMPLETED;
         this.reservations.remove(reservation);
         this.outstanding--;
      }
   }

   private void cancelLocked(Reservation reservation) {
      reservation.state = ReservationState.CANCELLED;
      this.reservations.remove(reservation);
      this.outstanding--;
   }

   private void requireOwner(Reservation reservation) {
      Objects.requireNonNull(reservation, "reservation");
      if (reservation.owner != this) {
         throw new IllegalArgumentException("Reservation belongs to another queue");
      }
   }

   static final class Reservation {
      private final BoundedCountdownTaskQueue owner;
      private final Runnable action;
      private final int delayPasses;
      private long readyAtPass;
      private ReservationState state = ReservationState.RESERVED;

      private Reservation(BoundedCountdownTaskQueue owner, Runnable action, int delayPasses) {
         this.owner = owner;
         this.action = action;
         this.delayPasses = delayPasses;
      }
   }

   private enum ReservationState {
      RESERVED,
      QUEUED,
      RUNNING,
      COMPLETED,
      CANCELLED
   }
}
