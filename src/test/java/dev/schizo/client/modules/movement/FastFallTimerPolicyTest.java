package dev.schizo.client.modules.movement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FastFallTimerPolicyTest {
   @Test
   void keepsTimerOnlyWhileStrictModeAndFallConditionsRemainValid() {
      assertFalse(FastFallModule.shouldReleaseStrictTimer(true, true, false, false));
      assertFalse(FastFallModule.shouldReleaseStrictTimer(true, true, true, true));
      assertTrue(FastFallModule.shouldReleaseStrictTimer(false, true, false, false));
      assertTrue(FastFallModule.shouldReleaseStrictTimer(true, false, false, false));
      assertTrue(FastFallModule.shouldReleaseStrictTimer(true, true, true, false));
   }
}
