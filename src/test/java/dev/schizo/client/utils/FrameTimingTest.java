package dev.schizo.client.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FrameTimingTest {
   @Test
   void countsMoreThanTheOld4096SampleLimit() {
      FrameTiming timing = new FrameTiming();
      for (int frame = 0; frame < 5000; frame++) {
         timing.recordFrameAt(100_000_000L + frame * 100_000L);
      }
      assertEquals(5000, timing.getFramesPerSecond());
   }

   @Test
   void keepsTheExactOneSecondBoundaryAndExpiresOlderBuckets() {
      FrameTiming timing = new FrameTiming();
      timing.recordFrameAt(10_000_000L);
      timing.recordFrameAt(1_010_000_000L);
      assertEquals(2, timing.getFramesPerSecond());

      timing.recordFrameAt(1_011_000_001L);
      assertEquals(2, timing.getFramesPerSecond());
   }

   @Test
   void clearsTheWindowAfterLongIdleWithoutGrowingStorage() {
      FrameTiming timing = new FrameTiming();
      timing.recordFrameAt(1_000_000_000L);
      timing.recordFrameAt(10_000_000_000L);
      assertEquals(1, timing.getFramesPerSecond());
   }
}
