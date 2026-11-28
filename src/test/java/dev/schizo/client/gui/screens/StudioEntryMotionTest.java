package dev.schizo.client.gui.screens;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StudioEntryMotionTest {
   @Test void boundedMonotonicFadeReachesRest() {
      assertEquals(64, StudioEntryMotion.veilAlpha(-1));
      assertEquals(64, StudioEntryMotion.veilAlpha(0));
      int previous = 64;
      for (long elapsed = 0; elapsed <= StudioEntryMotion.DURATION_NANOS; elapsed += 1_000_000L) {
         int alpha = StudioEntryMotion.veilAlpha(elapsed);
         assertTrue(alpha >= 0 && alpha <= previous);
         previous = alpha;
      }
      assertEquals(0, previous);
      assertEquals(0, StudioEntryMotion.veilAlpha(Long.MAX_VALUE));
      assertEquals(8, StudioEntryMotion.veilAlpha(StudioEntryMotion.DURATION_NANOS / 2));
   }
}
