package dev.schizo.client.modules.movement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StepTimerPolicyTest {
   @Test
   void requestsTimerOnlyForNormalModeWithTimerEnabledOutsideCobwebs() {
      assertTrue(StepModule.shouldRequestStepTimer(true, true, false));
      assertFalse(StepModule.shouldRequestStepTimer(false, true, false));
      assertFalse(StepModule.shouldRequestStepTimer(true, false, false));
      assertFalse(StepModule.shouldRequestStepTimer(true, true, true));
   }
}
