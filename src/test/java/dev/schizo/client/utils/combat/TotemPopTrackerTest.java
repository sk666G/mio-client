package dev.schizo.client.utils.combat;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class TotemPopTrackerTest {
   @Test
   void disconnectExpiryUsesMonotonicElapsedTime() {
      long start = 10_000L;
      long timeout = TimeUnit.SECONDS.toNanos(120L);

      assertFalse(TotemPopTracker.isDisconnectExpired(start, start + timeout - 1L));
      assertFalse(TotemPopTracker.isDisconnectExpired(start, start + timeout));
      assertTrue(TotemPopTracker.isDisconnectExpired(start, start + timeout + 1L));
   }

   @Test
   void backwardOrEqualMonotonicInputDoesNotExpire() {
      assertFalse(TotemPopTracker.isDisconnectExpired(100L, 100L));
      assertFalse(TotemPopTracker.isDisconnectExpired(100L, 50L));
   }
}
