package dev.schizo.client.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MonotonicCountdownTest {
   @Test
   void subtractsElapsedMonotonicTime() {
      long start = 1_000_000_000L;
      long now = 3_000_000_000L;

      assertEquals(3.0F, MonotonicCountdown.remainingSeconds(start, now, 5.0F), 0.0001F);
   }

   @Test
   void clampsExpiredCountdownAtZero() {
      long start = 1_000_000_000L;
      long now = 8_000_000_000L;

      assertEquals(0.0F, MonotonicCountdown.remainingSeconds(start, now, 5.0F));
   }

   @Test
   void doesNotAdvanceForNonPositiveElapsedInput() {
      assertEquals(5.0F, MonotonicCountdown.remainingSeconds(10L, 10L, 5.0F));
      assertEquals(5.0F, MonotonicCountdown.remainingSeconds(10L, 5L, 5.0F));
   }

   @Test
   void nonPositiveDurationIsAlreadyExpired() {
      assertEquals(0.0F, MonotonicCountdown.remainingSeconds(0L, 1L, 0.0F));
      assertEquals(0.0F, MonotonicCountdown.remainingSeconds(0L, 1L, -1.0F));
   }
}
