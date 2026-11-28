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
   void reportsRollingAverageFpsAndFrameTimeVariance() {
      FrameTiming timing = new FrameTiming();
      timing.recordFrameAt(1_000_000_000L);
      timing.recordFrameAt(1_010_000_000L);
      timing.recordFrameAt(1_040_000_000L);

      assertEquals(50.0F, timing.getAverageFramesPerSecond(), 0.01F);
      assertEquals(100.0F, timing.getFrameTimeVarianceMillisSquared(), 0.01F);
   }

   @Test
   void exposesLongFrameDetectionUsingTheHitchThreshold() {
      FrameTiming timing = new FrameTiming();
      timing.recordFrameAt(1_000_000_000L);
      timing.recordFrameAt(1_049_000_000L);
      assertFalse(timing.isLastFrameLong());

      timing.recordFrameAt(1_100_000_000L);
      assertTrue(timing.isLastFrameLong());
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
   void reportsPointOnePercentLowOnlyWithEnoughTailSamples() {
      FrameTiming timing = new FrameTiming();
      long now = 0L;
      timing.recordFrameAt(now);
      for (int frame = 1; frame <= 100; frame++) {
         now += 10_000_000L;
         timing.recordFrameAt(now);
      }
      assertEquals(0.0F, timing.getOneTenthPercentLowFramesPerSecondAt(now));

      FrameTiming saturated = new FrameTiming();
      now = 0L;
      saturated.recordFrameAt(now);
      for (int frame = 1; frame <= 10_000; frame++) {
         now += frame <= 9_988 ? 98_000L : 1_000_000L;
         saturated.recordFrameAt(now);
      }

      assertEquals(1000.0F, saturated.getOneTenthPercentLowFramesPerSecondAt(now), 0.01F);
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
   void returnsRecentFrameTimesInChronologicalOrderAndLimitsToNewestSamples() {
      FrameTiming timing = new FrameTiming();
      timing.recordFrameAt(1_000_000_000L);
      timing.recordFrameAt(1_010_000_000L);
      timing.recordFrameAt(1_030_000_000L);
      timing.recordFrameAt(1_060_000_000L);
      timing.recordFrameAt(1_100_000_000L);

      assertArrayEquals(new float[]{10.0F, 20.0F, 30.0F, 40.0F},
         timing.getRecentFrameTimesMillis(10), 0.01F);
      assertArrayEquals(new float[]{30.0F, 40.0F},
         timing.getRecentFrameTimesMillis(2), 0.01F);

      float[] snapshot = timing.getRecentFrameTimesMillis(4);
      snapshot[0] = -1.0F;
      assertEquals(10.0F, timing.getRecentFrameTimesMillis(4)[0], 0.01F);
   }

   @Test
   void recentFrameHistoryReturnsAnEmptyArrayForEmptyOrNonPositiveRequests() {
      FrameTiming timing = new FrameTiming();
      assertArrayEquals(new float[0], timing.getRecentFrameTimesMillis(8));
      assertArrayEquals(new float[0], timing.getRecentFrameTimesMillis(0));

      timing.recordFrameAt(1_000_000_000L);
      timing.recordFrameAt(1_010_000_000L);
      assertArrayEquals(new float[0], timing.getRecentFrameTimesMillis(-1));
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

   @Test void rejectsInvalidFrameFactorScale() {
      var timing=new FrameTiming();
      assertEquals(0,timing.getFrameFactor(Float.NaN));
      assertEquals(0,timing.getFrameFactor(Float.POSITIVE_INFINITY));
      assertEquals(0,timing.getFrameFactor(-1));
   }

   @Test void longIdleInvalidatesCachedPercentileImmediately() {
      var timing=new FrameTiming();
      timing.recordFrameAt(0);
      timing.recordFrameAt(10_000_000);
      assertEquals(100,timing.getOnePercentLowFramesPerSecondAt(10_000_000),0.01);
      timing.recordFrameAt(2_000_000_000);
      assertEquals(1000.0/1990.0,timing.getOnePercentLowFramesPerSecondAt(20_000_000),0.001);
   }
}
