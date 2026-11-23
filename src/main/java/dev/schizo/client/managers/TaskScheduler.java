package dev.schizo.client.managers;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.MinecraftRunTickEvent;
import dev.schizo.client.events.RotationRequestEvent;
import dev.schizo.client.events.bus.EventBus;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.thread.BlockableEventLoop;

public final class TaskScheduler implements ClientAccess, AutoCloseable {
   public static final int MAX_ACCEPTED_TASKS = 512;
   public static final int MAX_TASK_WORK_PER_TICK = 64;

   private final ClientThreadTaskQueue.TaskCapacity capacity =
      new ClientThreadTaskQueue.TaskCapacity(MAX_ACCEPTED_TASKS);
   private final ClientThreadTaskQueue.TaskWorkBudget workBudget =
      new ClientThreadTaskQueue.TaskWorkBudget(MAX_TASK_WORK_PER_TICK);
   private final ClientThreadTaskQueue.Dispatcher dispatcher = new MinecraftClientDispatcher();
   private final ClientThreadTaskQueue rotationTasks = this.createQueue("rotation");
   private final ClientThreadTaskQueue runTickTasks = this.createQueue("run-tick");
   private final AtomicBoolean closed = new AtomicBoolean();

   public TaskScheduler() {
      ClientAccess.eventBus.subscribe(this);
   }

   private ClientThreadTaskQueue createQueue(String queueName) {
      return new ClientThreadTaskQueue(
         this.capacity,
         this.dispatcher,
         failure -> ClientAccess.logger.error("Scheduled {} task failed", queueName, failure),
         failure -> ClientAccess.logger.warn("Could not dispatch scheduled {} task", queueName, failure)
      );
   }

   /**
    * Legacy convenience API. Rejected work is reported through the client log.
    * Use {@link #tryScheduleRotationTask(Runnable, int)} when the caller needs a result.
    */
   public void scheduleRotationTask(Runnable task, int delay) {
      if (this.tryScheduleRotationTask(task, delay).isEmpty()) {
         ClientAccess.logger.warn("Rejected rotation task because the scheduler is full or shutting down");
      }
   }

   public Optional<ScheduledTaskHandle> tryScheduleRotationTask(Runnable task, int delay) {
      if (this.closed.get()) {
         return Optional.empty();
      }
      return this.rotationTasks.trySchedule(task, delay);
   }

   /**
    * Legacy convenience API. Rejected work is reported through the client log.
    * Use {@link #tryScheduleRunTickTask(Runnable)} when the caller needs a result.
    */
   public void scheduleRunTickTask(Runnable task) {
      if (this.tryScheduleRunTickTask(task).isEmpty()) {
         ClientAccess.logger.warn("Rejected run-tick task because the scheduler is full or shutting down");
      }
   }

   public Optional<ScheduledTaskHandle> tryScheduleRunTickTask(Runnable task) {
      if (this.closed.get()) {
         return Optional.empty();
      }
      return this.runTickTasks.trySchedule(task, 0);
   }

   @EventHandler(priority = -100)
   private void onRotationEvent(RotationRequestEvent event) {
      if (!this.closed.get()) {
         this.rotationTasks.runPass(this.workBudget);
      }
   }

   @EventHandler(priority = -100)
   private void onMinecraftRunTickEvent(MinecraftRunTickEvent event) {
      if (!this.closed.get()) {
         this.workBudget.beginTick();
         this.runTickTasks.runPass(this.workBudget);
      }
   }

   public void scheduleScreenChange(Screen screen) {
      if (this.tryScheduleRunTickTask(() -> ClientAccess.schizoMinecraft.setScreen(screen)).isEmpty()) {
         ClientAccess.logger.warn("Rejected scheduled screen change because the scheduler is full or shutting down");
      }
   }

   @Override
   public void close() {
      if (!this.closed.compareAndSet(false, true)) {
         return;
      }
      EventBus eventBus = ClientAccess.eventBus;
      if (eventBus != null) {
         eventBus.unsubscribe(this);
      }
      int cancelled = this.rotationTasks.closeAndCancelPending() + this.runTickTasks.closeAndCancelPending();
      if (cancelled > 0) {
         ClientAccess.logger.warn(
            "Cancelled {} accepted scheduled tasks during client shutdown",
            cancelled
         );
      }
   }

   public interface ScheduledTaskHandle {
      /** Cancels a task that has not started. Returns false once it is running or complete. */
      boolean cancel();

      boolean isDone();

      boolean isCancelled();
   }

   private static final class MinecraftClientDispatcher implements ClientThreadTaskQueue.Dispatcher {
      @Override
      public boolean isClientThread() {
         return Minecraft.getInstance().isSameThread();
      }

      @Override
      @SuppressWarnings("unchecked")
      public boolean execute(Runnable work) {
         // Minecraft's event loop has no acceptance return value; shutdown is guarded by close hooks below.
         ((BlockableEventLoop<Runnable>)Minecraft.getInstance()).executeIfPossible(work);
         return true;
      }
   }
}
