package dev.schizo.client.events;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.events.bus.EventBus;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ConnectScreenConnectEventTest {
   @Test
   void cancellationStopsLaterConnectionListeners() {
      EventBus eventBus = new EventBus();
      AtomicInteger laterListenerCalls = new AtomicInteger();
      eventBus.subscribe(new CancellingListener());
      eventBus.subscribe(new LaterListener(laterListenerCalls));

      ConnectScreenConnectEvent event = new ConnectScreenConnectEvent("example.invalid:25565");
      eventBus.postCancellable(event);

      assertTrue(event.isCancelled());
      assertEquals(0, laterListenerCalls.get());
   }

   private static final class CancellingListener {
      @EventHandler(priority = 100)
      private void beforeConnect(ConnectScreenConnectEvent event) {
         event.cancel();
      }
   }

   private static final class LaterListener {
      private final AtomicInteger calls;

      private LaterListener(AtomicInteger calls) {
         this.calls = calls;
      }

      @EventHandler
      private void observeConnect(ConnectScreenConnectEvent event) {
         this.calls.incrementAndGet();
      }
   }
}
