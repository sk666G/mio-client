package dev.schizo.client.events.bus;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.events.EventHandler;
import dev.schizo.client.profiling.EventProfiler;
import dev.schizo.client.profiling.RuntimeDiagnostics;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class EventBusLifecycleTest {
   @Test
   void subscribingTheSameObjectTwiceDoesNotDuplicateCallbacks() {
      EventProfiler profiler = new EventProfiler(() -> 0L);
      EventBus bus = new EventBus(profiler);
      CountingListener listener = new CountingListener();

      bus.subscribe(listener);
      bus.subscribe(listener);
      bus.post(new TickEvent());

      assertEquals(1, listener.calls.get());
      assertEquals(1, profiler.registeredCallbackCount());
   }

   @Test
   void unsubscribeOfNeverSubscribedObjectDoesNotRegisterProfilerCallbacks() {
      EventProfiler profiler = new EventProfiler(() -> 0L);
      EventBus bus = new EventBus(profiler);

      bus.unsubscribe(new CountingListener());

      assertEquals(0, profiler.registeredCallbackCount());
   }

   @Test
   void listenerCanBeUnsubscribedAndResubscribedExactlyOnce() {
      EventBus bus = new EventBus(new EventProfiler(() -> 0L));
      CountingListener listener = new CountingListener();

      bus.subscribe(listener);
      bus.unsubscribe(listener);
      bus.post(new TickEvent());
      assertEquals(0, listener.calls.get());

      bus.subscribe(listener);
      bus.post(new TickEvent());
      assertEquals(1, listener.calls.get());
   }

   @Test
   void failingListenerIsIdentifiedBeforeTheExceptionPropagates() {
      EventProfiler profiler = new EventProfiler(() -> 0L);
      RuntimeDiagnostics diagnostics = new RuntimeDiagnostics();
      EventBus bus = new EventBus(profiler, diagnostics);
      FailingListener listener = new FailingListener();
      bus.subscribe(listener);

      IllegalStateException failure =
         assertThrows(IllegalStateException.class, () -> bus.post(new TickEvent()));

      assertEquals("listener failure", failure.getMessage());
      RuntimeDiagnostics.Snapshot snapshot = diagnostics.snapshot();
      assertEquals(1L, snapshot.totalFailures());
      assertEquals(RuntimeDiagnostics.Area.EVENT_DISPATCH, snapshot.lastFailure().area());
      assertTrue(snapshot.lastFailure().detail().contains("FailingListener#onTick"));
      assertTrue(snapshot.lastFailure().detail().contains("TickEvent"));
   }

   @Test
   void directListenerSubscriptionIsAlsoIdempotent() {
      EventBus bus = new EventBus(new EventProfiler(() -> 0L));
      AtomicInteger calls = new AtomicInteger();
      EventListener listener = new EventListener() {
         @Override public boolean isStatic() { return false; }
         @Override public Class<?> getEventType() { return TickEvent.class; }
         @Override public int getPriority() { return 0; }
         @Override public void call(Object event) { calls.incrementAndGet(); }
      };

      bus.subscribeListener(listener);
      bus.subscribeListener(listener);
      bus.post(new TickEvent());

      assertEquals(1, calls.get());
      bus.unsubscribeListener(listener);
      bus.post(new TickEvent());
      assertEquals(1, calls.get());
   }

   private static final class TickEvent {
   }

   private static final class FailingListener {
      @EventHandler
      private void onTick(TickEvent event) {
         throw new IllegalStateException("listener failure");
      }
   }

   private static final class CountingListener {
      private final AtomicInteger calls = new AtomicInteger();

      @EventHandler
      private void onTick(TickEvent event) {
         this.calls.incrementAndGet();
      }
   }
}
