package dev.schizo.client.events.bus;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.diagnostics.RuntimeDiagnostics;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.profiling.EventProfiler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EventBusDiagnosticsTest {
   @AfterEach
   void resetDiagnostics() {
      RuntimeDiagnostics.clear();
   }

   @Test
   void attributesListenerFailureAndStillPropagatesTheOriginalException() {
      EventBus bus = new EventBus(new EventProfiler(() -> 0L));
      RuntimeException expected = new RuntimeException("boom");
      bus.subscribe(new ThrowingListener(expected));

      RuntimeException actual = assertThrows(RuntimeException.class, () -> bus.post(new TickEvent()));

      assertSame(expected, actual);
      RuntimeDiagnostics.FailureRecord failure = RuntimeDiagnostics.recentFailures().getLast();
      assertEquals(RuntimeDiagnostics.Area.EVENT, failure.area());
      assertTrue(failure.owner().contains("ThrowingListener#onTick"));
      assertTrue(failure.owner().contains("TickEvent"));
   }

   private static final class TickEvent {
   }

   private static final class ThrowingListener {
      private final RuntimeException failure;

      private ThrowingListener(RuntimeException failure) {
         this.failure = failure;
      }

      @EventHandler
      private void onTick(TickEvent event) {
         throw this.failure;
      }
   }
}
