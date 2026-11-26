package dev.schizo.client.events.bus;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.events.EventHandler;
import dev.schizo.client.profiling.EventProfiler;
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
   void unsubscribeReleasesSubscriberCacheAndResubscribeReusesProfilerHandle() {
      EventProfiler profiler = new EventProfiler(() -> 0L);
      EventBus bus = new EventBus(profiler);
      CountingListener listener = new CountingListener();

      bus.subscribe(listener);
      assertEquals(1, bus.cachedSubscriberCount());
      assertEquals(1, profiler.registeredCallbackCount());

      bus.unsubscribe(listener);
      assertEquals(0, bus.cachedSubscriberCount());

      bus.subscribe(listener);
      assertEquals(1, bus.cachedSubscriberCount());
      assertEquals(1, profiler.registeredCallbackCount());

      bus.post(new TickEvent());
      assertEquals(1, listener.calls.get());
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

   @Test
   void listenerFailuresAreAttributedAndStillPropagate() {
      EventProfiler profiler = new EventProfiler(() -> 0L);
      EventFailureTracker failures = new EventFailureTracker(4);
      EventBus bus = new EventBus(profiler, failures);
      FailingListener listener = new FailingListener();

      bus.subscribe(listener);
      IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> bus.post(new TickEvent()));

      assertEquals("boom", thrown.getMessage());
      assertEquals(1L, failures.totalFailures());
      EventFailureTracker.FailureSnapshot snapshot = failures.recentFailures().getFirst();
      assertTrue(snapshot.listener().contains("FailingListener"));
      assertEquals("TickEvent", snapshot.eventType());
      assertEquals("IllegalStateException", snapshot.failureType());
   }

   private static final class TickEvent {
   }

   private static final class FailingListener {
      @EventHandler
      private void onTick(TickEvent event) {
         throw new IllegalStateException("boom");
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
