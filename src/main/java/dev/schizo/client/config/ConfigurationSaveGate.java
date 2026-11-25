package dev.schizo.client.config;

import java.util.Objects;

/**
 * Orders asynchronous and synchronous configuration writes.
 *
 * <p>An asynchronous save reserves a generation and may write only while that
 * generation is still current. A synchronous save invalidates every pending
 * asynchronous generation and runs under the same exclusive gate.</p>
 */
public final class ConfigurationSaveGate {
   private long generation;

   public synchronized long reserveAsyncSave() {
      return ++this.generation;
   }

   public synchronized void runIfCurrent(long reservedGeneration, Runnable action) {
      Objects.requireNonNull(action, "action");
      if (reservedGeneration == this.generation) {
         action.run();
      }
   }

   public synchronized void invalidatePendingAndRun(Runnable action) {
      Objects.requireNonNull(action, "action");
      this.generation++;
      action.run();
   }
}
