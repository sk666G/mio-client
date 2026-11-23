package dev.schizo.client.managers;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * A bounded queue whose callbacks are promoted and executed on the client thread.
 * Cross-thread reservations consume capacity before dispatcher submission.
 */
final class ClientThreadTaskQueue implements AutoCloseable {
   interface Dispatcher {
      boolean isClientThread();

      /** Must enqueue on the client thread; false means no work was accepted. */
      boolean execute(Runnable work);
   }

   private enum State {
      RESERVED,
      QUEUED,
      RUNNING,
      DONE,
      CANCELLED
   }

   private final Object lock = new Object();
   private final ReentrantLock crossThreadDispatchLock = new ReentrantLock();
   private final ArrayDeque<TaskRecord> queue = new ArrayDeque<>();
   private final Set<TaskRecord> records = Collections.newSetFromMap(new IdentityHashMap<>());
   private final TaskCapacity capacity;
   private final Dispatcher dispatcher;
   private final Consumer<RuntimeException> taskFailureHandler;
   private final Consumer<RuntimeException> dispatchFailureHandler;
   private boolean closed;
   private long nextSequence;

   ClientThreadTaskQueue(
      TaskCapacity capacity,
      Dispatcher dispatcher,
      Consumer<RuntimeException> taskFailureHandler,
      Consumer<RuntimeException> dispatchFailureHandler
   ) {
      this.capacity = capacity;
      this.dispatcher = dispatcher;
      this.taskFailureHandler = taskFailureHandler;
      this.dispatchFailureHandler = dispatchFailureHandler;
   }

   Optional<TaskScheduler.ScheduledTaskHandle> trySchedule(Runnable task, int delay) {
      if (task == null) {
         throw new NullPointerException("task");
      }
      if (delay < 0) {
         throw new IllegalArgumentException("Task delay must be non-negative");
      }

      if (this.dispatcher.isClientThread()) {
         synchronized (this.lock) {
            if (this.closed || !this.capacity.tryReserve()) {
               return Optional.empty();
            }
            TaskRecord record = new TaskRecord(this, task, (long)delay, State.QUEUED, this.nextSequence++);
            this.records.add(record);
            this.queue.addLast(record);
            return Optional.of(record);
         }
      }

      this.crossThreadDispatchLock.lock();
      try {
         TaskRecord record;
         synchronized (this.lock) {
            if (this.closed || !this.capacity.tryReserve()) {
               return Optional.empty();
            }
            record = new TaskRecord(this, task, (long)delay, State.RESERVED, this.nextSequence++);
            this.records.add(record);
            this.queue.addLast(record);
         }

         try {
            if (!this.dispatcher.execute(() -> this.promote(record))) {
               this.cancel(record);
               this.dispatchFailureHandler.accept(new RejectedExecutionException(
                  "Client task executor rejected scheduled work"
               ));
               return Optional.empty();
            }
         } catch (RuntimeException rejection) {
            this.cancel(record);
            this.dispatchFailureHandler.accept(rejection);
            return Optional.empty();
         }
         return Optional.of(record);
      } finally {
         this.crossThreadDispatchLock.unlock();
      }
   }

   private void promote(TaskRecord record) {
      if (!this.dispatcher.isClientThread()) {
         this.cancel(record);
         this.dispatchFailureHandler.accept(
            new IllegalStateException("Client executor ran a promoted task off the client thread")
         );
         return;
      }
      synchronized (this.lock) {
         if (record.state == State.RESERVED && !this.closed) {
            record.state = State.QUEUED;
         }
      }
   }

   int runPass(TaskWorkBudget workBudget) {
      if (this.capacity.outstanding() == 0 || workBudget.remaining() == 0) {
         return 0;
      }
      if (!this.dispatcher.isClientThread()) {
         throw new IllegalStateException("Scheduled tasks must run on the client thread");
      }

      long sequenceBoundary;
      int queuedAtPassStart;
      synchronized (this.lock) {
         sequenceBoundary = this.nextSequence;
         queuedAtPassStart = this.queue.size();
      }

      int inspected = 0;
      int executed = 0;
      int passLimit = Math.min(workBudget.maximum(), queuedAtPassStart);
      while (inspected < passLimit) {
         TaskRecord record;
         synchronized (this.lock) {
            record = this.queue.peekFirst();
            if (record == null || record.sequence >= sequenceBoundary || record.state == State.RESERVED
               || !workBudget.tryConsume()) {
               break;
            }
            this.queue.removeFirst();
            if (record.state != State.QUEUED) {
               inspected++;
               continue;
            }
            if (record.remainingPasses > 0) {
               record.remainingPasses--;
               this.queue.addLast(record);
               inspected++;
               continue;
            }
            record.state = State.RUNNING;
         }
         inspected++;
         executed++;
         try {
            record.task.run();
         } catch (RuntimeException failure) {
            this.taskFailureHandler.accept(failure);
         } finally {
            synchronized (this.lock) {
               record.state = State.DONE;
               this.records.remove(record);
               this.capacity.release();
            }
         }
      }
      return executed;
   }

   private boolean cancel(TaskRecord record) {
      synchronized (this.lock) {
         if (record.state != State.RESERVED && record.state != State.QUEUED) {
            return false;
         }
         record.state = State.CANCELLED;
         this.queue.remove(record);
         this.records.remove(record);
         this.capacity.release();
         return true;
      }
   }

   private boolean isDone(TaskRecord record) {
      synchronized (this.lock) {
         return record.state == State.DONE || record.state == State.CANCELLED;
      }
   }

   private boolean isCancelled(TaskRecord record) {
      synchronized (this.lock) {
         return record.state == State.CANCELLED;
      }
   }

   int closeAndCancelPending() {
      synchronized (this.lock) {
         if (this.closed) {
            return 0;
         }
         this.closed = true;
         int cancelled = 0;
         Iterator<TaskRecord> iterator = this.records.iterator();
         while (iterator.hasNext()) {
            TaskRecord record = iterator.next();
            if (record.state == State.RESERVED || record.state == State.QUEUED) {
               record.state = State.CANCELLED;
               iterator.remove();
               this.capacity.release();
               cancelled++;
            }
         }
         this.queue.clear();
         return cancelled;
      }
   }

   @Override
   public void close() {
      this.closeAndCancelPending();
   }

   static final class TaskCapacity {
      private final int limit;
      private final AtomicInteger outstanding = new AtomicInteger();

      TaskCapacity(int limit) {
         if (limit <= 0) {
            throw new IllegalArgumentException("Task capacity must be positive");
         }
         this.limit = limit;
      }

      boolean tryReserve() {
         while (true) {
            int current = this.outstanding.get();
            if (current >= this.limit) {
               return false;
            }
            if (this.outstanding.compareAndSet(current, current + 1)) {
               return true;
            }
         }
      }

      void release() {
         int remaining = this.outstanding.decrementAndGet();
         if (remaining < 0) {
            this.outstanding.incrementAndGet();
            throw new IllegalStateException("Task capacity released more than once");
         }
      }

      int outstanding() {
         return this.outstanding.get();
      }
   }

   static final class TaskWorkBudget {
      private final int maximum;
      private int remaining;

      TaskWorkBudget(int maximum) {
         if (maximum <= 0) {
            throw new IllegalArgumentException("Work budget must be positive");
         }
         this.maximum = maximum;
         this.remaining = maximum;
      }

      void beginTick() {
         this.remaining = this.maximum;
      }

      int remaining() {
         return this.remaining;
      }

      int maximum() {
         return this.maximum;
      }

      boolean tryConsume() {
         if (this.remaining <= 0) {
            return false;
         }
         this.remaining--;
         return true;
      }
   }

   private static final class TaskRecord implements TaskScheduler.ScheduledTaskHandle {
      private final ClientThreadTaskQueue owner;
      private final Runnable task;
      private long remainingPasses;
      private final long sequence;
      private State state;

      private TaskRecord(
         ClientThreadTaskQueue owner, Runnable task, long remainingPasses, State state, long sequence
      ) {
         this.owner = owner;
         this.task = task;
         this.remainingPasses = remainingPasses;
         this.state = state;
         this.sequence = sequence;
      }

      @Override
      public boolean cancel() {
         return this.owner.cancel(this);
      }

      @Override
      public boolean isDone() {
         return this.owner.isDone(this);
      }

      @Override
      public boolean isCancelled() {
         return this.owner.isCancelled(this);
      }
   }
}
