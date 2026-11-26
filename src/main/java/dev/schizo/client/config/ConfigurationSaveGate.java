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
   public static final long NO_ASYNC_SAVE = -1L;

   private long generation;
   private boolean asyncClosed;

   public synchronized long reserveAsyncSave() {
      if (this.asyncClosed) {
         return NO_ASYNC_SAVE;
      }
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

   /** Permanently rejects new asynchronous saves and invalidates pending generations. */
   public synchronized void closeAsyncSaves() {
      if (this.asyncClosed) {
         return;
      }
      this.asyncClosed = true;
      this.generation++;
   }
}
