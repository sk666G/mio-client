package dev.schizo.client.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MonotonicTimeTest {
   @Test
   void detectsElapsedDurationAtBoundary() {
      long start = 1_000L;

      assertFalse(MonotonicTime.hasElapsed(start, 1_499L, 500L));
      assertTrue(MonotonicTime.hasElapsed(start, 1_500L, 500L));
      assertFalse(MonotonicTime.isWithin(start, 1_500L, 500L));
   }

   @Test
   void nonPositiveDurationIsImmediatelyElapsed() {
      assertTrue(MonotonicTime.hasElapsed(10L, 10L, 0L));
      assertTrue(MonotonicTime.hasElapsed(10L, 5L, -1L));
   }

   @Test
   void subtractionRemainsCorrectAcrossSignedLongWraparound() {
      long start = Long.MAX_VALUE - 5L;
      long now = Long.MIN_VALUE + 4L;

      assertTrue(MonotonicTime.hasElapsed(start, now, 10L));
      assertFalse(MonotonicTime.hasElapsed(start, now, 11L));
   }

   @Test
   void elapsedDurationClampsBackwardTimeAndHandlesWraparound() {
      assertEquals(250L, MonotonicTime.elapsedNanos(1_000L, 1_250L));
      assertEquals(0L, MonotonicTime.elapsedNanos(1_000L, 900L));

      long start = Long.MAX_VALUE - 5L;
      long now = Long.MIN_VALUE + 4L;
      assertEquals(10L, MonotonicTime.elapsedNanos(start, now));
   }

   @Test
   void backwardTimestampDoesNotCountAsElapsedForPositiveDuration() {
      assertFalse(MonotonicTime.hasElapsed(100L, 90L, 5L));
      assertTrue(MonotonicTime.isWithin(100L, 90L, 5L));
   }
}
