package dev.schizo.client.config;

import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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
   private final Set<String> protectedRestoredConfigs = new HashSet<>();

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

   /**
    * Serializes a disk restore against in-flight writes and invalidates queued saves.
    * The successfully restored config stays write-protected until the next client startup;
    * live in-memory config is deliberately not reloaded by the restore command.
    */
   public synchronized void restoreAndProtect(String configName, IoAction restoration) throws IOException {
      Objects.requireNonNull(configName, "configName");
      Objects.requireNonNull(restoration, "restoration");
      this.generation++;
      restoration.run();
      this.protectedRestoredConfigs.add(configName);
   }

   public synchronized boolean isSaveSuspended(String configName) {
      return this.protectedRestoredConfigs.contains(configName);
   }

   @FunctionalInterface
   public interface IoAction {
      void run() throws IOException;
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
