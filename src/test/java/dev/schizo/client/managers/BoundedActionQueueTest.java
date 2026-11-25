package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BoundedActionQueueTest {
   @Test
   void preservesFifoOrderAcrossMultipleBoundedDrains() {
      BoundedActionQueue queue = new BoundedActionQueue(600);
      List<Integer> executed = new ArrayList<>();
      for (int index = 0; index < 501; index++) {
         int action = index;
         assertTrue(queue.offer(() -> executed.add(action)));
      }

      assertEquals(500, queue.drain(500, Runnable::run));
      assertEquals(1, queue.size());
      assertEquals(500, executed.size());
      assertEquals(0, executed.getFirst());

      assertEquals(1, queue.drain(500, Runnable::run));
      assertEquals(501, executed.size());
      assertEquals(500, executed.getLast());
      assertTrue(queue.isEmpty());
   }

   @Test
   void rejectsNewActionsWhenFullWithoutDiscardingAcceptedActions() {
      BoundedActionQueue queue = new BoundedActionQueue(2);
      List<Integer> executed = new ArrayList<>();

      assertTrue(queue.offer(() -> executed.add(1)));
      assertTrue(queue.offer(() -> executed.add(2)));
      assertFalse(queue.offer(() -> executed.add(3)));
      assertTrue(queue.isFull());

      assertEquals(2, queue.drain(10, Runnable::run));
      assertEquals(List.of(1, 2), executed);
   }

   @Test
   void doesNotAllowReentrantDrainToReorderPendingActions() {
      BoundedActionQueue queue = new BoundedActionQueue(4);
      List<Integer> executed = new ArrayList<>();
      queue.offer(() -> {
         executed.add(1);
         assertEquals(0, queue.drain(4, action -> action.run()));
      });
      queue.offer(() -> executed.add(2));

      assertEquals(1, queue.drain(1, Runnable::run));
      assertEquals(List.of(1), executed);
      assertEquals(1, queue.size());

      assertEquals(1, queue.drain(1, Runnable::run));
      assertEquals(List.of(1, 2), executed);
   }

   @Test
   void restoresDrainStateAfterAnActionThrowsAndKeepsLaterActions() {
      BoundedActionQueue queue = new BoundedActionQueue(3);
      queue.offer(() -> {
         throw new IllegalStateException("test failure");
      });
      List<Integer> executed = new ArrayList<>();
      queue.offer(() -> executed.add(2));

      assertThrows(IllegalStateException.class, () -> queue.drain(10, Runnable::run));
      assertEquals(1, queue.size());
      assertEquals(1, queue.drain(10, Runnable::run));
      assertEquals(List.of(2), executed);
   }

   @Test
   void concurrentProducersRespectCapacityAndPreserveEveryAcceptedAction() throws Exception {
      int producerCount = 8;
      int actionsPerProducer = 1_000;
      int capacity = 2_048;
      BoundedActionQueue queue = new BoundedActionQueue(capacity);
      java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
      java.util.concurrent.ExecutorService producers =
         java.util.concurrent.Executors.newFixedThreadPool(producerCount);
      java.util.concurrent.atomic.AtomicInteger nextId = new java.util.concurrent.atomic.AtomicInteger();
      List<Integer> executed = new ArrayList<>();
      List<java.util.concurrent.Future<List<Integer>>> submissions = new ArrayList<>();

      try {
         for (int producer = 0; producer < producerCount; producer++) {
            submissions.add(producers.submit(() -> {
               start.await();
               List<Integer> accepted = new ArrayList<>();
               for (int index = 0; index < actionsPerProducer; index++) {
                  int id = nextId.getAndIncrement();
                  if (queue.offer(() -> executed.add(id))) {
                     accepted.add(id);
                  }
               }
               return accepted;
            }));
         }

         start.countDown();
         java.util.Set<Integer> expected = new java.util.HashSet<>();
         for (java.util.concurrent.Future<List<Integer>> submission : submissions) {
            expected.addAll(submission.get());
         }

         assertTrue(expected.size() <= capacity);
         assertEquals(expected.size(), queue.size());
         assertEquals(expected.size() == capacity, queue.isFull());

         while (!queue.isEmpty()) {
            queue.drain(37, Runnable::run);
         }

         assertEquals(expected.size(), executed.size());
         assertEquals(expected, new java.util.HashSet<>(executed));
      } finally {
         producers.shutdownNow();
      }
   }

   @Test
   void concurrentProducersAndDrainerDoNotLoseAcceptedActions() throws Exception {
      int producerCount = 6;
      int actionsPerProducer = 3_000;
      BoundedActionQueue queue = new BoundedActionQueue(128);
      java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
      java.util.concurrent.atomic.AtomicInteger remainingProducers =
         new java.util.concurrent.atomic.AtomicInteger(producerCount);
      java.util.concurrent.atomic.AtomicInteger nextId = new java.util.concurrent.atomic.AtomicInteger();
      java.util.Set<Integer> accepted = java.util.concurrent.ConcurrentHashMap.newKeySet();
      java.util.Set<Integer> executed = java.util.concurrent.ConcurrentHashMap.newKeySet();
      java.util.concurrent.ExecutorService workers =
         java.util.concurrent.Executors.newFixedThreadPool(producerCount + 1);
      List<java.util.concurrent.Future<?>> submissions = new ArrayList<>();

      try {
         for (int producer = 0; producer < producerCount; producer++) {
            submissions.add(workers.submit(() -> {
               try {
                  start.await();
                  for (int index = 0; index < actionsPerProducer; index++) {
                     int id = nextId.getAndIncrement();
                     if (queue.offer(() -> executed.add(id))) {
                        accepted.add(id);
                     }
                  }
               } finally {
                  remainingProducers.decrementAndGet();
               }
               return null;
            }));
         }

         java.util.concurrent.Future<?> drainer = workers.submit(() -> {
            start.await();
            while (remainingProducers.get() > 0 || !queue.isEmpty()) {
               if (queue.drain(17, Runnable::run) == 0) {
                  Thread.yield();
               }
            }
            return null;
         });

         start.countDown();
         for (java.util.concurrent.Future<?> submission : submissions) {
            submission.get();
         }
         drainer.get();

         assertFalse(accepted.isEmpty());
         assertEquals(accepted, executed);
         assertTrue(queue.isEmpty());
      } finally {
         workers.shutdownNow();
      }
   }

   @Test
   void rejectsInvalidCapacityAndNullActions() {
      assertThrows(IllegalArgumentException.class, () -> new BoundedActionQueue(0));
      BoundedActionQueue queue = new BoundedActionQueue(1);
      assertThrows(NullPointerException.class, () -> queue.offer(null));
   }

   @Test
   void clearDiscardsPendingActionsAndQueueRemainsReusable() {
      BoundedActionQueue queue = new BoundedActionQueue(3);
      List<Integer> executed = new ArrayList<>();
      queue.offer(() -> executed.add(1));
      queue.offer(() -> executed.add(2));

      assertEquals(2, queue.clear());
      assertTrue(queue.isEmpty());
      assertEquals(0, queue.drain(10, Runnable::run));
      assertTrue(executed.isEmpty());

      assertTrue(queue.offer(() -> executed.add(3)));
      assertEquals(1, queue.drain(10, Runnable::run));
      assertEquals(List.of(3), executed);
   }

   @Test
   void clearDuringDrainKeepsTheRunningActionButDropsLaterActions() {
      BoundedActionQueue queue = new BoundedActionQueue(3);
      List<Integer> executed = new ArrayList<>();
      queue.offer(() -> {
         executed.add(1);
         assertEquals(1, queue.clear());
      });
      queue.offer(() -> executed.add(2));

      assertEquals(1, queue.drain(10, Runnable::run));
      assertEquals(List.of(1), executed);
      assertTrue(queue.isEmpty());
   }
}
