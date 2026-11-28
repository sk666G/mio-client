package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.ClientAccess;
import org.junit.jupiter.api.Test;

class TaskSchedulerLifecycleTest {
   @Test
   void explicitShutdownRejectsNewWorkWithoutMinecraftThreadAccess() {
      TaskScheduler scheduler = new TaskScheduler();
      try {
         scheduler.shutdown();
         scheduler.shutdown();

         assertFalse(scheduler.scheduleRotationTask(() -> fail("must not run"), 0));
         assertFalse(scheduler.scheduleRunTickTask(() -> fail("must not run")));

         TaskScheduler.Snapshot snapshot = scheduler.snapshot();
         assertEquals(0, snapshot.totalQueued());
         assertEquals(0, snapshot.totalOutstanding());
      } finally {
         ClientAccess.eventBus.unsubscribe(scheduler);
      }
   }
}
