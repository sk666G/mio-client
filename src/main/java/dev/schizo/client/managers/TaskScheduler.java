/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.MinecraftRunTickEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.events.RotationRequestEvent;
import dev.schizo.client.events.bus.EventBus;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.thread.BlockableEventLoop;

public final class TaskScheduler implements ClientAccess {
   private final ClientThreadTaskQueue rotationTasks = new ClientThreadTaskQueue("rotation");
   private final ClientThreadTaskQueue runTickTasks = new ClientThreadTaskQueue("run-tick");

   public TaskScheduler() {
      EventBus eventBus = ClientAccess.eventBus;
      eventBus.subscribe(this);
   }

   public boolean scheduleRotationTask(Runnable task, int delay) {
      return this.scheduleRotationTask(null, task, delay);
   }

   public boolean scheduleRotationTask(Object owner, Runnable task, int delay) {
      return this.rotationTasks.enqueue(owner, task, delay);
   }

   public boolean scheduleRunTickTask(Runnable task) {
      return this.scheduleRunTickTask(null, task);
   }

   public boolean scheduleRunTickTask(Object owner, Runnable task) {
      return this.runTickTasks.enqueue(owner, task, 1);
   }

   public int cancelTasks(Object owner) {
      java.util.Objects.requireNonNull(owner, "owner");
      return this.rotationTasks.cancelOwned(owner) + this.runTickTasks.cancelOwned(owner);
   }

   public QueueSnapshot snapshot() {
      return new QueueSnapshot(
         this.rotationTasks.queuedCount(),
         this.rotationTasks.outstandingCount(),
         this.runTickTasks.queuedCount(),
         this.runTickTasks.outstandingCount()
      );
   }

   public record QueueSnapshot(
      int rotationQueued,
      int rotationOutstanding,
      int runTickQueued,
      int runTickOutstanding
   ) {
      public int totalQueued() {
         return this.rotationQueued + this.runTickQueued;
      }

      public int totalOutstanding() {
         return this.rotationOutstanding + this.runTickOutstanding;
      }
   }

   @EventHandler(priority = -100)
   private void onRotationEvent(RotationRequestEvent event) {
      this.rotationTasks.run();
   }

   @EventHandler(priority = -100)
   private void onMinecraftRunTickEvent(MinecraftRunTickEvent event) {
      this.runTickTasks.run();
   }

   public boolean scheduleScreenChange(Screen screen) {
      return this.scheduleRunTickTask(() -> ClientAccess.schizoMinecraft.setScreen(screen));
   }

   @EventHandler
   private void onMinecraftDisconnect(MinecraftDisconnectEvent event) {
      this.rotationTasks.clear();
      this.runTickTasks.clear();
   }

   /** Stops accepting work and discards tasks that have not started. Running tasks are not interrupted. */
   public void shutdown() {
      this.rotationTasks.shutdown();
      this.runTickTasks.shutdown();
   }

   private static final class ClientThreadTaskQueue {
      private final String name;
      private final BoundedCountdownTaskQueue tasks = new BoundedCountdownTaskQueue(
         BoundedCountdownTaskQueue.DEFAULT_CAPACITY,
         BoundedCountdownTaskQueue.DEFAULT_MAX_TASKS_PER_PASS
      );
      private final AtomicBoolean capacityWarningLogged = new AtomicBoolean();

      private ClientThreadTaskQueue(String name) {
         this.name = name;
      }

      private boolean enqueue(Object owner, Runnable task, int delay) {
         BoundedCountdownTaskQueue.Reservation reservation = this.tasks.reserve(owner, task, delay);
         if (reservation == null) {
            this.warnCapacity();
            return false;
         }

         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft.isSameThread()) {
            boolean accepted = this.tasks.enqueue(reservation);
            if (accepted) {
               this.capacityWarningLogged.set(false);
            }
            return accepted;
         }

         if (!minecraft.isRunning()) {
            this.tasks.cancel(reservation);
            this.shutdown();
            ClientAccess.logger.warn("Rejected {} task because the Minecraft client is stopping", this.name);
            return false;
         }

         try {
            ((BlockableEventLoop<Runnable>)minecraft).executeIfPossible(() -> this.tasks.enqueue(reservation));
            this.capacityWarningLogged.set(false);
            return true;
         } catch (RuntimeException dispatchFailure) {
            this.tasks.cancel(reservation);
            ClientAccess.logger.warn("Could not dispatch {} task to the Minecraft client thread", this.name, dispatchFailure);
            return false;
         }
      }

      private int cancelOwned(Object owner) {
         return this.tasks.cancelOwned(owner);
      }

      private int queuedCount() {
         return this.tasks.queuedCount();
      }

      private int outstandingCount() {
         return this.tasks.outstandingCount();
      }

      private void run() {
         this.tasks.runPass(failure -> ClientAccess.logger.error("Scheduled {} task failed", this.name, failure));
      }

      private void clear() {
         this.tasks.clear();
         this.capacityWarningLogged.set(false);
      }

      private void shutdown() {
         this.tasks.close();
      }

      private void warnCapacity() {
         if (this.capacityWarningLogged.compareAndSet(false, true)) {
            ClientAccess.logger.warn(
               "The {} task queue reached its capacity ({}); rejecting additional tasks until space is available",
               this.name,
               BoundedCountdownTaskQueue.DEFAULT_CAPACITY
            );
         }
      }
   }
}
