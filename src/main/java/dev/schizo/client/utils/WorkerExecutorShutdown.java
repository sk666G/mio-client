package dev.schizo.client.utils;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/** Gives daemon worker tasks a bounded chance to finish durable work during process shutdown. */
public final class WorkerExecutorShutdown {
   static final long DEFAULT_TIMEOUT_SECONDS = 2L;

   private WorkerExecutorShutdown() {
   }

   public static boolean shutdownAndAwait(ExecutorService executor) {
      return shutdownAndAwait(executor, DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
   }

   static boolean shutdownAndAwait(ExecutorService executor, long timeout, TimeUnit unit) {
      Objects.requireNonNull(executor, "executor");
      Objects.requireNonNull(unit, "unit");
      if (timeout < 0L) {
         throw new IllegalArgumentException("Shutdown timeout must not be negative");
      }

      executor.shutdown();
      try {
         return executor.awaitTermination(timeout, unit);
      } catch (InterruptedException interrupted) {
         Thread.currentThread().interrupt();
         return false;
      }
   }
}
