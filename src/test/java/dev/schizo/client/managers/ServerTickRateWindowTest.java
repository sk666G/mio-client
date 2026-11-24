package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ServerTickRateWindowTest {
   @Test
   void keepsExactlyTwentySamples() {
      ServerTickRateWindow window = new ServerTickRateWindow();
      for (int i = 0; i < 25; i++) {
         window.recordIntervalNanos(1_000_000_000L);
      }
      assertEquals(ServerTickRateWindow.CAPACITY, window.sampleCount());
   }

   @Test
   void computesAndClampsTickRateFromMonotonicIntervals() {
      ServerTickRateWindow window = new ServerTickRateWindow();

      window.recordIntervalNanos(1_000_000_000L);
      assertEquals(20.0F, window.latest(), 0.000001F);

      window.clear();
      window.recordIntervalNanos(2_000_000_000L);
      assertEquals(10.0F, window.latest(), 0.000001F);
      assertEquals(10.0F, window.average(), 0.000001F);
   }

   @Test
   void ignoresNonPositiveIntervalsAndClearResetsState() {
      ServerTickRateWindow window = new ServerTickRateWindow();
      window.recordIntervalNanos(0L);
      window.recordIntervalNanos(-1L);
      assertEquals(0, window.sampleCount());

      window.recordIntervalNanos(2_000_000_000L);
      window.clear();
      assertEquals(0, window.sampleCount());
      assertEquals(0.0F, window.latest(), 0.000001F);
      assertEquals(0.0F, window.average(), 0.000001F);
   }

   @Test
   void nearTwentyAverageUsesStableTwentyDisplayValue() {
      ServerTickRateWindow window = new ServerTickRateWindow();
      window.recordIntervalNanos(1_001_000_000L);
      assertEquals(20.0F, window.average(), 0.000001F);
   }
}
