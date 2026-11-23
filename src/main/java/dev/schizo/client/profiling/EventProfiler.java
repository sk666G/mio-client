package dev.schizo.client.profiling;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.LongSupplier;

/**
 * Opt-in event callback and direct HUD widget profiler.
 *
 * Callback times are inclusive: a callback that synchronously posts another event includes
 * the nested dispatch time. The separate HUD measurements include their widget render call.
 * These values overlap and must not be summed as a frame-time total.
 */
public final class EventProfiler {
   public static final int MAX_CALLBACKS = 4096;
   public static final int MAX_MODULES = 512;
   public static final int MAX_HUD_WIDGETS = 256;
   public static final EventProfiler GLOBAL = new EventProfiler(System::nanoTime);

   private final LongSupplier clock;
   private final CallbackHandle[] callbacks = new CallbackHandle[MAX_CALLBACKS];
   private final ModuleHandle[] modules = new ModuleHandle[MAX_MODULES];
   private final HudHandle[] hudWidgets = new HudHandle[MAX_HUD_WIDGETS];
   private volatile Epoch epoch = new Epoch(MAX_CALLBACKS, MAX_MODULES, MAX_HUD_WIDGETS);
   private volatile boolean enabled;
   private int callbackCount;
   private int moduleCount;
   private int hudWidgetCount;

   public EventProfiler(LongSupplier clock) {
      this.clock = java.util.Objects.requireNonNull(clock, "clock");
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void start() {
      this.enabled = true;
   }

   public void stop() {
      this.enabled = false;
   }

   public long nanoTime() {
      return this.clock.getAsLong();
   }

   public synchronized CallbackHandle registerCallback(
      String ownerName, String moduleName, String methodName, String eventTypeName
   ) {
      String owner = nonEmpty(ownerName, "Unknown listener");
      String label = owner + "#" + nonEmpty(methodName, "unknown") + " [" + nonEmpty(eventTypeName, "Unknown event") + "]";
      int moduleIndex = moduleName == null ? -1 : this.findOrRegisterModule(moduleName);
      if (this.callbackCount >= this.callbacks.length) {
         return new CallbackHandle(this, -1, moduleIndex, label, moduleName);
      }
      int index = this.callbackCount++;
      CallbackHandle handle = new CallbackHandle(this, index, moduleIndex, label, moduleName);
      this.callbacks[index] = handle;
      this.epoch.callbackCounters[index] = new Counters();
      return handle;
   }

   public synchronized HudHandle registerHudWidget(String widgetName) {
      String name = nonEmpty(widgetName, "Unknown HUD widget");
      for (int i = 0; i < this.hudWidgetCount; i++) {
         if (this.hudWidgets[i].name.equals(name)) return this.hudWidgets[i];
      }
      if (this.hudWidgetCount >= this.hudWidgets.length) return new HudHandle(this, -1, name);
      int index = this.hudWidgetCount++;
      HudHandle handle = new HudHandle(this, index, name);
      this.hudWidgets[index] = handle;
      this.epoch.hudCounters[index] = new Counters();
      return handle;
   }

   private int findOrRegisterModule(String name) {
      for (int i = 0; i < this.moduleCount; i++) {
         if (this.modules[i].name.equals(name)) return i;
      }
      if (this.moduleCount >= this.modules.length) return -1;
      int index = this.moduleCount++;
      this.modules[index] = new ModuleHandle(name);
      this.epoch.moduleCounters[index] = new Counters();
      return index;
   }

   public void recordCallback(CallbackHandle handle, long elapsedNanos) {
      if (handle == null || handle.owner != this) return;
      Epoch current = this.epoch;
      long elapsed = Math.max(0L, elapsedNanos);
      if (handle.index >= 0) current.callbackCounters[handle.index].record(elapsed);
      if (handle.moduleIndex >= 0) current.moduleCounters[handle.moduleIndex].record(elapsed);
   }

   public void recordHudRender(HudHandle handle, long elapsedNanos) {
      if (handle == null || handle.owner != this || handle.index < 0) return;
      Epoch current = this.epoch;
      current.hudCounters[handle.index].record(Math.max(0L, elapsedNanos));
   }

   /**
    * Swaps counter epochs without taking a lock in dispatch. A callback already in flight
    * when reset occurs may finish against the prior epoch and be omitted from the new results.
    */
   public synchronized void reset() {
      Epoch next = new Epoch(MAX_CALLBACKS, MAX_MODULES, MAX_HUD_WIDGETS);
      for (int i = 0; i < this.callbackCount; i++) next.callbackCounters[i] = new Counters();
      for (int i = 0; i < this.moduleCount; i++) next.moduleCounters[i] = new Counters();
      for (int i = 0; i < this.hudWidgetCount; i++) next.hudCounters[i] = new Counters();
      this.epoch = next;
   }

   public synchronized List<CallbackSnapshot> topCallbacks(int limit) {
      Epoch current = this.epoch;
      List<CallbackSnapshot> results = new ArrayList<>();
      for (int i = 0; i < this.callbackCount; i++) {
         Counters counters = current.callbackCounters[i];
         long calls = counters.count.sum();
         if (calls == 0) continue;
         results.add(new CallbackSnapshot(this.callbacks[i].label, calls,
            counters.totalNanos.sum(), counters.maximumNanos.get()));
      }
      results.sort(Comparator.comparingLong(CallbackSnapshot::totalNanos).reversed());
      return limitedCopy(results, limit);
   }

   public synchronized List<ModuleSnapshot> topModules(int limit) {
      Epoch current = this.epoch;
      List<ModuleSnapshot> results = new ArrayList<>();
      for (int i = 0; i < this.moduleCount; i++) {
         Counters counters = current.moduleCounters[i];
         long calls = counters.count.sum();
         if (calls == 0) continue;
         results.add(new ModuleSnapshot(this.modules[i].name, calls,
            counters.totalNanos.sum(), counters.maximumNanos.get()));
      }
      results.sort(Comparator.comparingLong(ModuleSnapshot::totalNanos).reversed());
      return limitedCopy(results, limit);
   }

   public synchronized List<HudSnapshot> topHudWidgets(int limit) {
      Epoch current = this.epoch;
      List<HudSnapshot> results = new ArrayList<>();
      for (int i = 0; i < this.hudWidgetCount; i++) {
         Counters counters = current.hudCounters[i];
         long calls = counters.count.sum();
         if (calls == 0) continue;
         results.add(new HudSnapshot(this.hudWidgets[i].name, calls,
            counters.totalNanos.sum(), counters.maximumNanos.get()));
      }
      results.sort(Comparator.comparingLong(HudSnapshot::totalNanos).reversed());
      return limitedCopy(results, limit);
   }

   public synchronized int registeredCallbackCount() {
      return this.callbackCount;
   }

   private static <T> List<T> limitedCopy(List<T> results, int limit) {
      int end = Math.min(results.size(), Math.max(0, limit));
      return List.copyOf(results.subList(0, end));
   }

   private static String nonEmpty(String value, String fallback) {
      return value == null || value.isBlank() ? fallback : value;
   }

   private static final class Epoch {
      private final Counters[] callbackCounters;
      private final Counters[] moduleCounters;
      private final Counters[] hudCounters;

      private Epoch(int callbackCapacity, int moduleCapacity, int hudCapacity) {
         this.callbackCounters = new Counters[callbackCapacity];
         this.moduleCounters = new Counters[moduleCapacity];
         this.hudCounters = new Counters[hudCapacity];
      }
   }

   private static final class Counters {
      private final LongAdder count = new LongAdder();
      private final LongAdder totalNanos = new LongAdder();
      private final AtomicLong maximumNanos = new AtomicLong();

      private void record(long elapsed) {
         this.count.increment();
         this.totalNanos.add(elapsed);
         long maximum = this.maximumNanos.get();
         while (elapsed > maximum && !this.maximumNanos.compareAndSet(maximum, elapsed)) {
            maximum = this.maximumNanos.get();
         }
      }
   }

   private static final class ModuleHandle {
      private final String name;
      private ModuleHandle(String name) { this.name = name; }
   }

   public static final class CallbackHandle {
      private final EventProfiler owner;
      private final int index;
      private final int moduleIndex;
      private final String label;
      private final String moduleName;

      private CallbackHandle(EventProfiler owner, int index, int moduleIndex, String label, String moduleName) {
         this.owner = owner;
         this.index = index;
         this.moduleIndex = moduleIndex;
         this.label = label;
         this.moduleName = moduleName;
      }
   }

   public static final class HudHandle {
      private final EventProfiler owner;
      private final int index;
      private final String name;

      private HudHandle(EventProfiler owner, int index, String name) {
         this.owner = owner;
         this.index = index;
         this.name = name;
      }
   }

   public record CallbackSnapshot(String label, long invocationCount, long totalNanos, long maximumNanos) {
      public long averageNanos() { return this.invocationCount == 0 ? 0 : this.totalNanos / this.invocationCount; }
   }

   public record ModuleSnapshot(String moduleName, long invocationCount, long totalNanos, long maximumNanos) {
      public long averageNanos() { return this.invocationCount == 0 ? 0 : this.totalNanos / this.invocationCount; }
   }

   public record HudSnapshot(String widgetName, long invocationCount, long totalNanos, long maximumNanos) {
      public long averageNanos() { return this.invocationCount == 0 ? 0 : this.totalNanos / this.invocationCount; }
   }
}
