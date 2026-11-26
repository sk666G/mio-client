package dev.schizo.client.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class WorkerExecutorShutdownTest {
   @Test
   void waitsForQueuedWorkerToFinishAfterShutdown() throws Exception {
      var executor = Executors.newSingleThreadExecutor();
      CountDownLatch completed = new CountDownLatch(1);
      executor.submit(completed::countDown);

      assertTrue(WorkerExecutorShutdown.shutdownAndAwait(executor, 1L, TimeUnit.SECONDS));
      assertTrue(completed.await(0L, TimeUnit.MILLISECONDS));
      assertTrue(executor.isShutdown());
      assertTrue(executor.isTerminated());
   }

   @Test
   void returnsFalseWhenWorkExceedsDeadline() throws Exception {
      var executor = Executors.newSingleThreadExecutor();
      CountDownLatch release = new CountDownLatch(1);
      CountDownLatch started = new CountDownLatch(1);
      executor.submit(() -> {
         started.countDown();
         try {
            release.await();
         } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
         }
      });
      assertTrue(started.await(1L, TimeUnit.SECONDS));

      try {
         assertFalse(WorkerExecutorShutdown.shutdownAndAwait(executor, 0L, TimeUnit.MILLISECONDS));
         assertTrue(executor.isShutdown());
      } finally {
         release.countDown();
         assertTrue(executor.awaitTermination(1L, TimeUnit.SECONDS));
      }
   }
}
