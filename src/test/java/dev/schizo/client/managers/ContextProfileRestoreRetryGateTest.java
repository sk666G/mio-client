package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class ContextProfileRestoreRetryGateTest {
   @Test
   void failedRestoreCanRetryAfterBoundedDelay() {
      AtomicLong now = new AtomicLong(100);
      ContextProfileRestoreRetryGate gate = new ContextProfileRestoreRetryGate(now::get, 1000);

      assertTrue(gate.isReady());
      gate.recordFailure();
      assertFalse(gate.isReady());
      now.set(1099);
      assertFalse(gate.isReady());
      now.set(1100);
      assertTrue(gate.isReady());
   }

   @Test
   void successfulRestoreClearsBackoffForTheNextTransition() {
      AtomicLong now = new AtomicLong(100);
      ContextProfileRestoreRetryGate gate = new ContextProfileRestoreRetryGate(now::get, 1000);
      gate.recordFailure();

      gate.recordSuccess();

      assertTrue(gate.isReady());
   }

   @Test
   void rejectsAnInvalidRetryDelay() {
      assertThrows(
         IllegalArgumentException.class,
         () -> new ContextProfileRestoreRetryGate(() -> 0L, 0L)
      );
   }
}
