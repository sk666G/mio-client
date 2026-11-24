package dev.schizo.client.modules.combat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AntiPhaseSwapPolicyTest {
   @Test
   void capturesOnlyTheFirstOriginalSlotForABatch() {
      assertTrue(AntiPhaseSwapPolicy.shouldCaptureOriginal(false));
      assertFalse(AntiPhaseSwapPolicy.shouldCaptureOriginal(true));
   }
}
