package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.Test;

class ClientThreadTaskQueueTest {
   @Test
   void capacityRejectsOverflowAndReleasesReservationsAfterExecution() {
      ClientThreadTaskQueue.TaskCapacity capacity = new ClientThreadTaskQueue.TaskCapacity(2);
      FakeDispatcher dispatcher = new FakeDispatcher();
      ClientThreadTaskQueue queue = newQueue(capacity, dispatcher);
      List<Integer> ran = new ArrayList<>();

      assertTrue(queue.trySchedule(() -> ran.add(1), 0).isPresent());
      assertTrue(queue.trySchedule(() -> ran.add(2), 0).isPresent());
      assertTrue(queue.trySchedule(() -> ran.add(3), 0).isEmpty());
      assertEquals(2, capacity.outstanding());

      assertEquals(2, queue.runPass(new ClientThreadTaskQueue.TaskWorkBudget(8)));
      assertEquals(List.of(1, 2), ran);
      assertEquals(0, capacity.outstanding());
   }

   @Test
   void reservesCapacityBeforeCrossThreadDispatch() {
      ClientThreadTaskQueue.TaskCapacity capacity = new ClientThreadTaskQueue.TaskCapacity(1);
      FakeDispatcher dispatcher = new FakeDispatcher();
      dispatcher.clientThread = false;
      dispatcher.duringDispatch = () -> assertEquals(1, capacity.outstanding());
      ClientThreadTaskQueue queue = newQueue(capacity, dispatcher);
      List<String> ran = new ArrayList<>();

      Optional<TaskScheduler.ScheduledTaskHandle> accepted =
         queue.trySchedule(() -> ran.add("accepted"), 0);
      assertTrue(accepted.isPresent());
      assertTrue(queue.trySchedule(() -> ran.add("overflow"), 0).isEmpty());
      assertEquals(1, capacity.outstanding());

      dispatcher.clientThread = true;
      dispatcher.runDispatched();
      queue.runPass(new ClientThreadTaskQueue.TaskWorkBudget(8));
      assertEquals(List.of("accepted"), ran);
      assertEquals(0, capacity.outstanding());
   }

   @Test
   void preservesFifoOrderForReadyTasks() {
      ClientThreadTaskQueue.TaskCapacity capacity = new ClientThreadTaskQueue.TaskCapacity(8);
      ClientThreadTaskQueue queue = newQueue(capacity, new FakeDispatcher());
      List<Integer> ran = new ArrayList<>();
      queue.trySchedule(() -> ran.add(1), 0);
      queue.trySchedule(() -> ran.add(2), 0);
      queue.trySchedule(() -> ran.add(3), 0);

      queue.runPass(new ClientThreadTaskQueue.TaskWorkBudget(8));
      assertEquals(List.of(1, 2, 3), ran);
   }

   @Test
   void waitsTheConfiguredNumberOfPassesBeforeRunningDelayedWork() {
      ClientThreadTaskQueue.TaskCapacity capacity = new ClientThreadTaskQueue.TaskCapacity(4);
      ClientThreadTaskQueue queue = newQueue(capacity, new FakeDispatcher());
      List<String> ran = new ArrayList<>();
      queue.trySchedule(() -> ran.add("later"), 2);

      queue.runPass(new ClientThreadTaskQueue.TaskWorkBudget(4));
      assertTrue(ran.isEmpty());
      queue.runPass(new ClientThreadTaskQueue.TaskWorkBudget(4));
      assertTrue(ran.isEmpty());
      queue.runPass(new ClientThreadTaskQueue.TaskWorkBudget(4));
      assertEquals(List.of("later"), ran);
      assertEquals(0, capacity.outstanding());
   }

   @Test
   void tasksScheduledByCallbacksWaitForTheNextPass() {
      ClientThreadTaskQueue.TaskCapacity capacity = new ClientThreadTaskQueue.TaskCapacity(8);
      ClientThreadTaskQueue queue = newQueue(capacity, new FakeDispatcher());
      List<Integer> ran = new ArrayList<>();
      queue.trySchedule(() -> {
         ran.add(1);
         queue.trySchedule(() -> ran.add(3), 0);
      }, 0);
      queue.trySchedule(() -> ran.add(2), 0);

      assertEquals(2, queue.runPass(new ClientThreadTaskQueue.TaskWorkBudget(8)));
      assertEquals(List.of(1, 2), ran);
      assertEquals(1, capacity.outstanding());
      queue.runPass(new ClientThreadTaskQueue.TaskWorkBudget(8));
      assertEquals(List.of(1, 2, 3), ran);
      assertEquals(0, capacity.outstanding());
   }

   @Test
   void executorRejectionAndShutdownReleaseAcceptedCapacityExplicitly() {
      ClientThreadTaskQueue.TaskCapacity capacity = new ClientThreadTaskQueue.TaskCapacity(4);
      FakeDispatcher rejectingDispatcher = new FakeDispatcher();
      rejectingDispatcher.reject = true;
      List<RuntimeException> dispatchFailures = new ArrayList<>();
      ClientThreadTaskQueue rejectedQueue = new ClientThreadTaskQueue(
         capacity, rejectingDispatcher, ignored -> {}, dispatchFailures::add
      );
      rejectingDispatcher.clientThread = false;

      assertTrue(rejectedQueue.trySchedule(() -> {}, 0).isEmpty());
      assertEquals(0, capacity.outstanding());
      assertEquals(1, dispatchFailures.size());

      FakeDispatcher dispatcher = new FakeDispatcher();
      dispatcher.clientThread = false;
      ClientThreadTaskQueue queue = newQueue(capacity, dispatcher);
      List<String> ran = new ArrayList<>();
      Optional<TaskScheduler.ScheduledTaskHandle> accepted = queue.trySchedule(() -> ran.add("no"), 0);
      assertTrue(accepted.isPresent());
      assertEquals(1, queue.closeAndCancelPending());
      assertTrue(accepted.orElseThrow().isCancelled());
      assertTrue(accepted.orElseThrow().isDone());
      assertEquals(0, capacity.outstanding());

      dispatcher.clientThread = true;
      dispatcher.runDispatched();
      queue.runPass(new ClientThreadTaskQueue.TaskWorkBudget(4));
      assertTrue(ran.isEmpty());
      assertTrue(queue.trySchedule(() -> ran.add("closed"), 0).isEmpty());
      assertEquals(0, capacity.outstanding());
   }

   @Test
   void dispatcherExceptionsReleaseTheReservation() {
      ClientThreadTaskQueue.TaskCapacity capacity = new ClientThreadTaskQueue.TaskCapacity(2);
      FakeDispatcher dispatcher = new FakeDispatcher();
      dispatcher.clientThread = false;
      dispatcher.throwOnDispatch = true;
      List<RuntimeException> failures = new ArrayList<>();
      ClientThreadTaskQueue queue = new ClientThreadTaskQueue(capacity, dispatcher, ignored -> {}, failures::add);

      assertTrue(queue.trySchedule(() -> {}, 0).isEmpty());
      assertEquals(0, capacity.outstanding());
      assertEquals(1, failures.size());
      assertTrue(failures.getFirst() instanceof RejectedExecutionException);
   }

   @Test
   void taskFailureIsReportedAndDoesNotBlockFollowingTasks() {
      ClientThreadTaskQueue.TaskCapacity capacity = new ClientThreadTaskQueue.TaskCapacity(4);
      List<RuntimeException> failures = new ArrayList<>();
      ClientThreadTaskQueue queue = new ClientThreadTaskQueue(
         capacity, new FakeDispatcher(), failures::add, ignored -> {}
      );
      List<String> ran = new ArrayList<>();
      queue.trySchedule(() -> {
         throw new IllegalArgumentException("expected test failure");
      }, 0);
      queue.trySchedule(() -> ran.add("continued"), 0);

      assertEquals(2, queue.runPass(new ClientThreadTaskQueue.TaskWorkBudget(4)));
      assertEquals(1, failures.size());
      assertEquals(List.of("continued"), ran);
      assertEquals(0, capacity.outstanding());
   }

   @Test
   void cancellationAndPerTickWorkBudgetKeepAccountingBounded() {
      ClientThreadTaskQueue.TaskCapacity capacity = new ClientThreadTaskQueue.TaskCapacity(8);
      ClientThreadTaskQueue queue = newQueue(capacity, new FakeDispatcher());
      List<Integer> ran = new ArrayList<>();
      Optional<TaskScheduler.ScheduledTaskHandle> cancelled = queue.trySchedule(() -> ran.add(0), 0);
      cancelled.orElseThrow().cancel();
      for (int value = 1; value <= 5; value++) {
         int captured = value;
         queue.trySchedule(() -> ran.add(captured), 0);
      }
      assertEquals(5, capacity.outstanding());

      ClientThreadTaskQueue.TaskWorkBudget budget = new ClientThreadTaskQueue.TaskWorkBudget(2);
      assertEquals(2, queue.runPass(budget));
      assertEquals(List.of(1, 2), ran);
      assertEquals(3, capacity.outstanding());
      budget.beginTick();
      assertEquals(2, queue.runPass(budget));
      assertEquals(List.of(1, 2, 3, 4), ran);
      budget.beginTick();
      assertEquals(1, queue.runPass(budget));
      assertEquals(List.of(1, 2, 3, 4, 5), ran);
      assertEquals(0, capacity.outstanding());
      assertFalse(cancelled.orElseThrow().cancel());
   }

   private static ClientThreadTaskQueue newQueue(
      ClientThreadTaskQueue.TaskCapacity capacity,
      FakeDispatcher dispatcher
   ) {
      return new ClientThreadTaskQueue(capacity, dispatcher, ignored -> {}, ignored -> {});
   }

   private static final class FakeDispatcher implements ClientThreadTaskQueue.Dispatcher {
      private boolean clientThread = true;
      private boolean reject;
      private boolean throwOnDispatch;
      private Runnable duringDispatch = () -> {};
      private final List<Runnable> dispatched = new ArrayList<>();

      @Override
      public boolean isClientThread() {
         return this.clientThread;
      }

      @Override
      public boolean execute(Runnable work) {
         this.duringDispatch.run();
         if (this.throwOnDispatch) {
            throw new RejectedExecutionException("test rejection");
         }
         if (this.reject) {
            return false;
         }
         this.dispatched.add(work);
         return true;
      }

      private void runDispatched() {
         List<Runnable> copy = List.copyOf(this.dispatched);
         this.dispatched.clear();
         copy.forEach(Runnable::run);
      }
   }
}
