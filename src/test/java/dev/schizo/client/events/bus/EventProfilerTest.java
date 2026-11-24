package dev.schizo.client.events.bus;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.events.EventHandler;
import dev.schizo.client.profiling.EventProfiler;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class EventProfilerTest {
   @Test
   void disabledDispatchNeverReadsTheClock() {
      AtomicInteger clockReads = new AtomicInteger();
      EventProfiler profiler = new EventProfiler(() -> {
         clockReads.incrementAndGet();
         return 1L;
      });
      EventBus bus = new EventBus(profiler);
      bus.subscribe(new BasicListener());

      bus.post(new TickEvent());

      assertEquals(0, clockReads.get());
      assertTrue(profiler.topCallbacks(10).isEmpty());
   }

   @Test
   void callbackAndModuleCountsTotalsAveragesAndMaximumsAreCorrect() {
      AtomicLong clock = new AtomicLong();
      EventProfiler profiler = new EventProfiler(() -> clock.getAndAdd(5L));
      EventBus bus = new EventBus(profiler);
      bus.subscribe(new BasicListener());
      profiler.start();

      bus.post(new TickEvent());
      bus.post(new TickEvent());

      EventProfiler.CallbackSnapshot callback = profiler.topCallbacks(1).get(0);
      assertEquals(2, callback.invocationCount());
      assertEquals(10, callback.totalNanos());
      assertEquals(5, callback.averageNanos());
      assertEquals(5, callback.maximumNanos());

      EventProfiler isolated = new EventProfiler(() -> 0L);
      EventProfiler.CallbackHandle handle = isolated.registerCallback("Aura", "Aura", "onTick", "TickEvent");
      isolated.recordCallback(handle, 12L);
      isolated.recordCallback(handle, 28L);
      EventProfiler.ModuleSnapshot module = isolated.topModules(1).get(0);
      assertEquals("Aura", module.moduleName());
      assertEquals(2, module.invocationCount());
      assertEquals(40, module.totalNanos());
      assertEquals(20, module.averageNanos());
      assertEquals(28, module.maximumNanos());
   }

   @Test
   void resetClearsCountersButKeepsRegisteredHandles() {
      EventProfiler profiler = new EventProfiler(() -> 0L);
      EventProfiler.CallbackHandle handle = profiler.registerCallback("Aura", "Aura", "onTick", "TickEvent");
      profiler.recordCallback(handle, 40L);

      profiler.reset();

      assertTrue(profiler.topCallbacks(10).isEmpty());
      assertTrue(profiler.topModules(10).isEmpty());
      profiler.recordCallback(handle, 9L);
      assertEquals(9L, profiler.topCallbacks(1).get(0).totalNanos());
   }

   @Test
   void callbackRegistryIsBounded() {
      EventProfiler profiler = new EventProfiler(() -> 0L);
      for (int index = 0; index < EventProfiler.MAX_CALLBACKS + 100; index++) {
         profiler.registerCallback("Listener" + index, null, "onTick", "TickEvent");
      }
      assertEquals(EventProfiler.MAX_CALLBACKS, profiler.registeredCallbackCount());
   }

   @Test
   void nestedDispatchRecordsInclusiveDurationsWithoutCorruptingEitherCallback() {
      AtomicLong clock = new AtomicLong();
      EventProfiler profiler = new EventProfiler(() -> clock.getAndIncrement());
      EventBus bus = new EventBus(profiler);
      NestedListener listener = new NestedListener(bus);
      bus.subscribe(listener);
      profiler.start();

      bus.post(new OuterEvent());

      List<EventProfiler.CallbackSnapshot> callbacks = profiler.topCallbacks(10);
      assertEquals(2, callbacks.size());
      assertEquals(3L, callbacks.stream().filter(row -> row.label().contains("onOuter")).findFirst().orElseThrow().totalNanos());
      assertEquals(1L, callbacks.stream().filter(row -> row.label().contains("onInner")).findFirst().orElseThrow().totalNanos());
   }

   @Test
   void exceptionsPropagateUnchangedAndStillRecordElapsedTime() {
      AtomicLong clock = new AtomicLong();
      EventProfiler profiler = new EventProfiler(() -> clock.getAndAdd(7L));
      EventBus bus = new EventBus(profiler);
      RuntimeException expected = new RuntimeException("same failure");
      bus.subscribe(new ThrowingListener(expected));
      profiler.start();

      RuntimeException actual = assertThrows(RuntimeException.class, () -> bus.post(new TickEvent()));

      assertSame(expected, actual);
      EventProfiler.CallbackSnapshot callback = profiler.topCallbacks(1).get(0);
      assertEquals(1, callback.invocationCount());
      assertEquals(7, callback.totalNanos());
      assertEquals(7, callback.maximumNanos());
   }

   @Test
   void concurrentDispatchUsesThreadSafeStatistics() throws Exception {
      EventProfiler profiler = new EventProfiler(System::nanoTime);
      EventBus bus = new EventBus(profiler);
      AtomicInteger invocations = new AtomicInteger();
      bus.subscribe(new ConcurrentListener(invocations));
      profiler.start();

      ExecutorService workers = Executors.newFixedThreadPool(4);
      try {
         Future<?>[] tasks = new Future<?>[4];
         for (int worker = 0; worker < tasks.length; worker++) {
            tasks[worker] = workers.submit(() -> {
               for (int call = 0; call < 1000; call++) bus.post(new TickEvent());
            });
         }
         for (Future<?> task : tasks) task.get();
      } finally {
         workers.shutdownNow();
      }

      assertEquals(4000, invocations.get());
      assertEquals(4000, profiler.topCallbacks(1).get(0).invocationCount());
   }

   @Test
   void hudRenderStatsAreSeparateAndAggregateByWidget() {
      EventProfiler profiler = new EventProfiler(() -> 0L);
      EventProfiler.HudHandle first = profiler.registerHudWidget("Watermark");
      EventProfiler.HudHandle sameWidget = profiler.registerHudWidget("Watermark");
      profiler.recordHudRender(first, 11);
      profiler.recordHudRender(sameWidget, 29);

      EventProfiler.HudSnapshot hud = profiler.topHudWidgets(1).get(0);
      assertEquals("Watermark", hud.widgetName());
      assertEquals(2, hud.invocationCount());
      assertEquals(40, hud.totalNanos());
      assertEquals(20, hud.averageNanos());
      assertEquals(29, hud.maximumNanos());
      assertTrue(profiler.topCallbacks(1).isEmpty());
   }

   private static class TickEvent { }
   private static class OuterEvent { }
   private static class InnerEvent { }

   private static class BasicListener {
      @EventHandler
      public void onTick(TickEvent event) { }
   }

   private static class ThrowingListener {
      private final RuntimeException failure;
      private ThrowingListener(RuntimeException failure) { this.failure = failure; }
      @EventHandler
      public void onTick(TickEvent event) { throw this.failure; }
   }

   private static class NestedListener {
      private final EventBus bus;
      private NestedListener(EventBus bus) { this.bus = bus; }
      @EventHandler
      public void onOuter(OuterEvent event) { this.bus.post(new InnerEvent()); }
      @EventHandler
      public void onInner(InnerEvent event) { }
   }

   private static class ConcurrentListener {
      private final AtomicInteger calls;
      private ConcurrentListener(AtomicInteger calls) { this.calls = calls; }
      @EventHandler
      public void onTick(TickEvent event) { this.calls.incrementAndGet(); }
   }
}
