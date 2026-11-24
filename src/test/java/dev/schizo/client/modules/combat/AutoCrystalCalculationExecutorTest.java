package dev.schizo.client.modules.combat;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class AutoCrystalCalculationExecutorTest {
   @Test
   void rejectsOverlappingScansInsteadOfQueueingThem() throws InterruptedException {
      ThreadPoolExecutor executor = AutoCrystalModule.createCalculationExecutor();
      CountDownLatch workerStarted = new CountDownLatch(1);
      CountDownLatch releaseWorker = new CountDownLatch(1);

      try {
         executor.execute(() -> {
            workerStarted.countDown();
            try {
               releaseWorker.await();
            } catch (InterruptedException interrupted) {
               Thread.currentThread().interrupt();
            }
         });

         assertTrue(workerStarted.await(5L, TimeUnit.SECONDS));
         assertThrows(
            RejectedExecutionException.class,
            () -> executor.execute(() -> { })
         );
         assertTrue(executor.getQueue().isEmpty());
      } finally {
         releaseWorker.countDown();
         executor.shutdownNow();
         assertTrue(executor.awaitTermination(5L, TimeUnit.SECONDS));
      }
   }
}
