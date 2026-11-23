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

   @Test
   void recordsFrameSequenceAndLatestFrameDuration() {
      FrameTiming timing = new FrameTiming();
      timing.recordFrameAt(1_000_000_000L);
      assertEquals(1L, timing.getFrameSequence());
      assertEquals(0.0F, timing.getFrameTimeMillis());

      timing.recordFrameAt(1_016_000_000L);
      assertEquals(2L, timing.getFrameSequence());
      assertEquals(16.0F, timing.getFrameTimeMillis(), 0.01F);
   }

   @Test
   void calculatesAverageFrameTimeFromTheOneSecondWindow() {
      FrameTiming timing = new FrameTiming();
      timing.recordFrameAt(1_000_000_000L);
      timing.recordFrameAt(1_016_000_000L);
      timing.recordFrameAt(1_032_000_000L);
      timing.recordFrameAt(1_048_000_000L);

      assertEquals(16.0F, timing.getAverageFrameTimeMillis(), 0.01F);
   }

   @Test
   void calculatesOnePercentLowFromTheNinetyNinthPercentile() {
      FrameTiming timing = new FrameTiming();
      timing.recordFrameAt(1_000_000_000L);
      for (int frame = 1; frame <= 10; frame++) {
         timing.recordFrameAt(1_000_000_000L + frame * 10_000_000L);
      }
      timing.recordFrameAt(1_150_000_000L);

      assertEquals(20.0F, timing.getOnePercentLowFramesPerSecondAt(2_000_000_000L), 0.01F);
      assertEquals(13.636F, timing.getAverageFrameTimeMillis(), 0.01F);
   }

   @Test
   void expiresOldFrameDurationsAfterOneSecond() {
      FrameTiming timing = new FrameTiming();
      timing.recordFrameAt(1_000_000_000L);
      timing.recordFrameAt(1_016_000_000L);
      timing.recordFrameAt(2_027_000_000L);

      assertEquals(1011.0F, timing.getAverageFrameTimeMillis(), 0.01F);
   }

   @Test
   void clearsFrameDurationHistoryWhenTheClockMovesBackwards() {
      FrameTiming timing = new FrameTiming();
      timing.recordFrameAt(1_000_000_000L);
      timing.recordFrameAt(1_016_000_000L);
      assertEquals(16.0F, timing.getAverageFrameTimeMillis(), 0.01F);

      timing.recordFrameAt(900_000_000L);
      assertEquals(0.0F, timing.getAverageFrameTimeMillis());
      assertEquals(0.0F, timing.getFrameTimeMillis());

      timing.recordFrameAt(916_000_000L);
      assertEquals(16.0F, timing.getAverageFrameTimeMillis(), 0.01F);
   }

   @Test
   void countsHitchesAndExpiresThemFromTheRollingWindow() {
      FrameTiming timing = new FrameTiming();
      timing.recordFrameAt(1_000_000_000L);
      timing.recordFrameAt(1_016_000_000L);
      timing.recordFrameAt(1_116_000_000L);
      timing.recordFrameAt(1_132_000_000L);

      assertEquals(1, timing.getStuttersPerSecond());

      for (int frame = 1; frame <= 64; frame++) {
         timing.recordFrameAt(1_132_000_000L + frame * 16_000_000L);
      }

      assertEquals(0, timing.getStuttersPerSecond());
   }
}
