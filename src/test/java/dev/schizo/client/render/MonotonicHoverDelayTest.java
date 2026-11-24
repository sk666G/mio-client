package dev.schizo.client.render;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MonotonicHoverDelayTest {
   @Test
   void firstHoverStartsTheDelayInsteadOfShowingImmediately() {
      MonotonicHoverDelay delay = new MonotonicHoverDelay(300L);

      assertFalse(delay.hasElapsed(10_000L));
      assertFalse(delay.hasElapsed(10_299L));
      assertTrue(delay.hasElapsed(10_300L));
   }

   @Test
   void leavingAndReenteringRestartsTheDelay() {
      MonotonicHoverDelay delay = new MonotonicHoverDelay(300L);

      assertFalse(delay.hasElapsed(1_000L));
      assertTrue(delay.hasElapsed(1_300L));
      delay.reset();
      assertFalse(delay.hasElapsed(5_000L));
      assertFalse(delay.hasElapsed(5_299L));
      assertTrue(delay.hasElapsed(5_300L));
   }

   @Test
   void zeroDelayIsVisibleOnFirstSample() {
      assertTrue(new MonotonicHoverDelay(0L).hasElapsed(123L));
   }

   @Test
   void elapsedCheckSurvivesSignedLongWraparound() {
      MonotonicHoverDelay delay = new MonotonicHoverDelay(50L);

      assertFalse(delay.hasElapsed(Long.MAX_VALUE - 20L));
      assertFalse(delay.hasElapsed(Long.MIN_VALUE + 19L));
      assertTrue(delay.hasElapsed(Long.MIN_VALUE + 30L));
   }

   @Test
   void rejectsNegativeDelays() {
      assertThrows(IllegalArgumentException.class, () -> new MonotonicHoverDelay(-1L));
   }
}
