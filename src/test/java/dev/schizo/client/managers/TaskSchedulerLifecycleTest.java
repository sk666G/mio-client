package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.ClientAccess;
import org.junit.jupiter.api.Test;

class TaskSchedulerLifecycleTest {
   @Test
   void explicitShutdownRejectsNewWorkWithoutMinecraftRuntimeState() {
      TaskScheduler scheduler = new TaskScheduler();
      try {
         scheduler.shutdown();

         assertTrue(scheduler.isShutdown());
         assertFalse(scheduler.scheduleRotationTask(() -> fail("must not run"), 0));
         assertFalse(scheduler.scheduleRunTickTask(() -> fail("must not run")));
         assertEquals(0, scheduler.snapshot().totalQueued());
         assertEquals(0, scheduler.snapshot().totalOutstanding());
      } finally {
         ClientAccess.eventBus.unsubscribe(scheduler);
      }
   }

   @Test
   void shutdownIsIdempotent() {
      TaskScheduler scheduler = new TaskScheduler();
      try {
         scheduler.shutdown();
         scheduler.shutdown();

         assertTrue(scheduler.isShutdown());
         assertFalse(scheduler.scheduleRotationTask(new Object(), () -> fail("must not run"), 1));
         assertFalse(scheduler.scheduleRunTickTask(new Object(), () -> fail("must not run")));
      } finally {
         ClientAccess.eventBus.unsubscribe(scheduler);
      }
   }
}
