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
   void rejectsInvalidCapacityAndNullActions() {
      assertThrows(IllegalArgumentException.class, () -> new BoundedActionQueue(0));
      BoundedActionQueue queue = new BoundedActionQueue(1);
      assertThrows(NullPointerException.class, () -> queue.offer(null));
   }
}
