package dev.schizo.client.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReconnectAttemptsTest {
   @Test
   void retryLimitSurvivesScreenReplacementAndResetsForAnotherServer() {
      ReconnectAttempts retries = new ReconnectAttempts();
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
   void backoffNeverShortensTheConfiguredInterval() {
      ReconnectAttempts retries = new ReconnectAttempts();
      retries.select("server");
      for (int i = 0; i < 100; i++) {
         retries.claim(100);
         assertEquals(900.0D, retries.delaySeconds(900.0D, true));
      }
   }

   @Test
   void backoffIsBoundedAndCountdownHandlesNanoTimeWrap() {
      ReconnectAttempts retries = new ReconnectAttempts();
      assertEquals(5.0D, retries.delaySeconds(5.0D, true));
      for (int i = 0; i < 100; i++) {
         retries.claim(100);
      }
      assertEquals(60.0D, retries.delaySeconds(5.0D, true));
      assertEquals(5.0D, retries.delaySeconds(5.0D, false));
      long start = Long.MAX_VALUE - 500_000_000L;
      assertEquals(1.0F, ReconnectAttempts.remainingSeconds(start, start + 1_000_000_000L, 2.0D));
   }
}
