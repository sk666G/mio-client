package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
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
   void reentrantDrainIsIgnored() {
      BoundedCountdownTaskQueue queue = new BoundedCountdownTaskQueue(2, 2);
      AtomicInteger nestedDrains = new AtomicInteger();
      assertTrue(queue.offer(() -> nestedDrains.set(queue.runPass(failure -> fail(failure))), 0));

      queue.runPass(failure -> fail(failure));
      queue.runPass(failure -> fail(failure));
      assertEquals(0, nestedDrains.get());
      assertEquals(0, queue.outstandingCount());
   }
}
