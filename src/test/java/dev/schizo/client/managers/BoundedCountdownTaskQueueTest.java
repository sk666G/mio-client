package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class BoundedCountdownTaskQueueTest {
   @Test
   void reservationsCountAgainstCapacityBeforeClientThreadDispatch() {
      BoundedCountdownTaskQueue queue = new BoundedCountdownTaskQueue(2, 2);
      BoundedCountdownTaskQueue.Reservation first = queue.reserve(() -> { }, 0);
      BoundedCountdownTaskQueue.Reservation second = queue.reserve(() -> { }, 0);

      assertNotNull(first);
      assertNotNull(second);
      assertNull(queue.reserve(() -> { }, 0));
      assertEquals(2, queue.outstandingCount());
      assertEquals(0, queue.queuedCount());

      assertTrue(queue.enqueue(first));
      assertTrue(queue.enqueue(second));
      assertEquals(2, queue.queuedCount());
      assertEquals(2, queue.outstandingCount());
   }

   @Test
   void cancellationReleasesAReservedSlot() {
      BoundedCountdownTaskQueue queue = new BoundedCountdownTaskQueue(1, 1);
      BoundedCountdownTaskQueue.Reservation reservation = queue.reserve(() -> { }, 0);

      assertNotNull(reservation);
      assertTrue(queue.cancel(reservation));
      assertEquals(0, queue.outstandingCount());
      assertNotNull(queue.reserve(() -> { }, 0));
      assertFalse(queue.cancel(reservation));
   }

   @Test
   void preservesFifoOrderAcrossBoundedPasses() {
      BoundedCountdownTaskQueue queue = new BoundedCountdownTaskQueue(600, 500);
      List<Integer> executed = new ArrayList<>();
      for (int index = 0; index < 501; index++) {
         int action = index;
         assertTrue(queue.offer(() -> executed.add(action), 0));
      }

      assertEquals(0, queue.runPass(failure -> fail(failure)));
      assertEquals(500, queue.runPass(failure -> fail(failure)));
      assertEquals(500, executed.size());
      assertEquals(1, queue.queuedCount());
      assertEquals(499, executed.getLast());

      assertEquals(1, queue.runPass(failure -> fail(failure)));
      assertEquals(501, executed.size());
      assertEquals(500, executed.getLast());
      assertEquals(0, queue.outstandingCount());
   }

   @Test
   void doesNotRunTasksAddedByAnActionUntilTheNextEligiblePass() {
      BoundedCountdownTaskQueue queue = new BoundedCountdownTaskQueue(4, 4);
      List<Integer> executed = new ArrayList<>();
      assertTrue(queue.offer(() -> {
         executed.add(1);
         assertTrue(queue.offer(() -> executed.add(3), 0));
      }, 0));
      assertTrue(queue.offer(() -> executed.add(2), 0));

      assertEquals(0, queue.runPass(failure -> fail(failure)));
      assertEquals(2, queue.runPass(failure -> fail(failure)));
      assertEquals(List.of(1, 2), executed);
      assertEquals(1, queue.queuedCount());

      assertEquals(0, queue.runPass(failure -> fail(failure)));
      assertEquals(1, queue.runPass(failure -> fail(failure)));
      assertEquals(List.of(1, 2, 3), executed);
   }

   @Test
   void removesFailingTasksAndContinuesWithLaterTasks() {
      BoundedCountdownTaskQueue queue = new BoundedCountdownTaskQueue(3, 3);
      AtomicInteger failures = new AtomicInteger();
      List<Integer> executed = new ArrayList<>();
      assertTrue(queue.offer(() -> {
         throw new IllegalStateException("expected test failure");
      }, 0));
      assertTrue(queue.offer(() -> executed.add(2), 0));

      assertEquals(0, queue.runPass(failure -> fail(failure)));
      assertEquals(2, queue.runPass(failure -> {
         assertInstanceOf(IllegalStateException.class, failure);
         failures.incrementAndGet();
      }));
      assertEquals(1, failures.get());
      assertEquals(List.of(2), executed);
      assertEquals(0, queue.outstandingCount());
   }

   @Test
   void rejectsTasksAfterCapacityIsReachedWithoutDroppingAcceptedWork() {
      BoundedCountdownTaskQueue queue = new BoundedCountdownTaskQueue(2, 2);
      List<Integer> executed = new ArrayList<>();

      assertTrue(queue.offer(() -> executed.add(1), 0));
      assertTrue(queue.offer(() -> executed.add(2), 0));
      assertFalse(queue.offer(() -> executed.add(3), 0));
      assertEquals(2, queue.outstandingCount());

      queue.runPass(failure -> fail(failure));
      assertEquals(2, queue.runPass(failure -> fail(failure)));
      assertEquals(List.of(1, 2), executed);
      assertEquals(0, queue.outstandingCount());
   }

   @Test
   void closeCancelsQueuedAndDispatchedButNotYetEnqueuedReservations() {
      BoundedCountdownTaskQueue queue = new BoundedCountdownTaskQueue(3, 3);
      BoundedCountdownTaskQueue.Reservation dispatched = queue.reserve(() -> fail("must not run"), 0);
      assertNotNull(dispatched);
      assertTrue(queue.offer(() -> fail("must not run"), 0));

      queue.close();

      assertTrue(queue.isClosed());
      assertEquals(0, queue.queuedCount());
      assertEquals(0, queue.outstandingCount());
      assertFalse(queue.enqueue(dispatched));
      assertNull(queue.reserve(() -> { }, 0));
      assertEquals(0, queue.runPass(failure -> fail(failure)));
   }

   @Test
   void preservesCountdownDelayAcrossPasses() {
      BoundedCountdownTaskQueue queue = new BoundedCountdownTaskQueue(2, 2);
      AtomicInteger executed = new AtomicInteger();
      assertTrue(queue.offer(executed::incrementAndGet, 1));

      assertEquals(0, queue.runPass(failure -> fail(failure)));
      assertEquals(0, queue.runPass(failure -> fail(failure)));
      assertEquals(0, executed.get());
      assertEquals(1, queue.runPass(failure -> fail(failure)));
      assertEquals(1, executed.get());
   }

   @Test
   void keepsLaterTasksBehindAnOlderDelayedTask() {
      BoundedCountdownTaskQueue queue = new BoundedCountdownTaskQueue(3, 3);
      List<Integer> executed = new ArrayList<>();
      assertTrue(queue.offer(() -> executed.add(1), 2));
      assertTrue(queue.offer(() -> executed.add(2), 0));

      assertEquals(0, queue.runPass(failure -> fail(failure)));
      assertEquals(0, queue.runPass(failure -> fail(failure)));
      assertEquals(0, queue.runPass(failure -> fail(failure)));
      assertEquals(2, queue.runPass(failure -> fail(failure)));
      assertEquals(List.of(1, 2), executed);
   }

   @Test
   void concurrentProducersCannotExceedReservationCapacityOrLoseAcceptedTasks() throws Exception {
      int producerCount = 8;
      int tasksPerProducer = 1_000;
      int capacity = 1_024;
      BoundedCountdownTaskQueue queue = new BoundedCountdownTaskQueue(capacity, 64);
      CountDownLatch start = new CountDownLatch(1);
      AtomicInteger nextId = new AtomicInteger();
      Set<Integer> accepted = ConcurrentHashMap.newKeySet();
      Set<Integer> executed = ConcurrentHashMap.newKeySet();
      ExecutorService producers = Executors.newFixedThreadPool(producerCount);
      List<Future<?>> submissions = new ArrayList<>();

      try {
         for (int producer = 0; producer < producerCount; producer++) {
            submissions.add(producers.submit(() -> {
               start.await();
               for (int index = 0; index < tasksPerProducer; index++) {
                  int id = nextId.getAndIncrement();
                  BoundedCountdownTaskQueue.Reservation reservation =
                     queue.reserve(() -> executed.add(id), 0);
                  if (reservation != null && queue.enqueue(reservation)) {
                     accepted.add(id);
                  }
               }
               return null;
            }));
         }

         start.countDown();
         for (Future<?> submission : submissions) {
            submission.get();
         }

         assertEquals(capacity, accepted.size());
         assertEquals(capacity, queue.outstandingCount());
         assertEquals(capacity, queue.queuedCount());

         while (queue.outstandingCount() > 0) {
            queue.runPass(failure -> fail(failure));
         }

         assertEquals(accepted, executed);
         assertEquals(0, queue.outstandingCount());
      } finally {
         producers.shutdownNow();
      }
   }

   @Test
   void rejectsInvalidCapacityDelayAndForeignReservations() {
      assertThrows(IllegalArgumentException.class, () -> new BoundedCountdownTaskQueue(0, 1));
      assertThrows(IllegalArgumentException.class, () -> new BoundedCountdownTaskQueue(1, 0));

      BoundedCountdownTaskQueue queue = new BoundedCountdownTaskQueue(1, 1);
      assertThrows(NullPointerException.class, () -> queue.reserve(null, 0));
      assertThrows(IllegalArgumentException.class, () -> queue.reserve(() -> { }, -1));

      BoundedCountdownTaskQueue anotherQueue = new BoundedCountdownTaskQueue(1, 1);
      BoundedCountdownTaskQueue.Reservation reservation = anotherQueue.reserve(() -> { }, 0);
      assertNotNull(reservation);
      assertThrows(IllegalArgumentException.class, () -> queue.enqueue(reservation));
      assertThrows(IllegalArgumentException.class, () -> queue.cancel(reservation));
   }

   @Test
   void cancellingAnOwnerRemovesOnlyItsPendingTasks() {
      BoundedCountdownTaskQueue queue = new BoundedCountdownTaskQueue(4, 4);
      Object firstOwner = new Object();
      Object secondOwner = new Object();
      AtomicInteger executed = new AtomicInteger();

      BoundedCountdownTaskQueue.Reservation first = queue.reserve(firstOwner, executed::incrementAndGet, 0);
      BoundedCountdownTaskQueue.Reservation second = queue.reserve(secondOwner, executed::incrementAndGet, 0);
      assertNotNull(first);
      assertNotNull(second);
      assertTrue(queue.enqueue(first));
      assertTrue(queue.enqueue(second));

      assertEquals(1, queue.cancelOwner(firstOwner));
      assertEquals(1, queue.outstandingCount());
      queue.runPass(failure -> fail(failure));
      assertEquals(1, queue.runPass(failure -> fail(failure)));
      assertEquals(1, executed.get());
   }

   @Test
   void reentrantDrainIsIgnored() {
      BoundedCountdownTaskQueue queue = new BoundedCountdownTaskQueue(2, 2);
      AtomicInteger nestedDrains = new AtomicInteger();
      assertTrue(queue.offer(() -> nestedDrains.set(queue.runPass(failure -> fail(failure))), 0));

      queue.runPass(failure -> fail(failure));
      queue.runPass(failure -> fail(failure));
      assertEquals(0, nestedDrains.get());
      assertEquals(0, queue.outstandingCount());
   }

   @Test
   void clearCancelsQueuedAndReservedTasksWithoutClosingTheQueue() {
      BoundedCountdownTaskQueue queue = new BoundedCountdownTaskQueue(4, 4);
      BoundedCountdownTaskQueue.Reservation reserved = queue.reserve(() -> fail("must not run"), 0);
      assertNotNull(reserved);
      assertTrue(queue.offer(() -> fail("must not run"), 0));

      assertEquals(2, queue.clear());
      assertEquals(0, queue.queuedCount());
      assertEquals(0, queue.outstandingCount());
      assertFalse(queue.isClosed());
      assertFalse(queue.enqueue(reserved));

      AtomicInteger executed = new AtomicInteger();
      assertTrue(queue.offer(executed::incrementAndGet, 0));
      queue.runPass(failure -> fail(failure));
      assertEquals(1, queue.runPass(failure -> fail(failure)));
      assertEquals(1, executed.get());
   }

   @Test
   void clearFromRunningTaskCancelsOnlyPendingWork() {
      BoundedCountdownTaskQueue queue = new BoundedCountdownTaskQueue(3, 3);
      List<Integer> executed = new ArrayList<>();
      assertTrue(queue.offer(() -> {
         executed.add(1);
         assertEquals(1, queue.clear());
      }, 0));
      assertTrue(queue.offer(() -> executed.add(2), 0));

      queue.runPass(failure -> fail(failure));
      assertEquals(1, queue.runPass(failure -> fail(failure)));
      assertEquals(List.of(1), executed);
      assertEquals(0, queue.outstandingCount());
   }
}
