package dev.schizo.client.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReconnectAttemptsTest {
   @Test
   void retryLimitSurvivesScreenReplacementAndResetsForAnotherServer() {
      var retries = new ReconnectAttempts();
      retries.select("first");
      assertTrue(retries.claim(2));
      retries.select("first");
      assertTrue(retries.claim(2));
      assertFalse(retries.claim(2));
      retries.select("second");
      assertTrue(retries.claim(2));
      retries.reset();
      assertTrue(retries.claim(1));
   }

   @Test
   void backoffIsBoundedAndCountdownHandlesNanoTimeWrap() {
      var retries = new ReconnectAttempts();
      assertEquals(5.0, retries.delaySeconds(5, true));
      for (int i = 0; i < 100; i++) retries.claim(100);
      assertEquals(60.0, retries.delaySeconds(5, true));
      assertEquals(5.0, retries.delaySeconds(5, false));
      long start = Long.MAX_VALUE - 500_000_000L;
      assertEquals(1.0F, MonotonicCountdown.remainingSeconds(start, start + 1_000_000_000L, 2));
   }
}
