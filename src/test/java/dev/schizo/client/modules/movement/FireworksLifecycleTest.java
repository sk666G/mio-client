package dev.schizo.client.modules.movement;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FireworksLifecycleTest {
   @Test
   void disabledModuleNeverRequestsFastLaunchJump() {
      assertFalse(
         FireworksModule.shouldRequestFastLaunchJump(
            false, true, true, FireworksModule.ModeChoice.TOGGLE
         )
      );
   }

   @Test
   void fastLaunchJumpRequiresEveryRuntimePrecondition() {
      assertFalse(
         FireworksModule.shouldRequestFastLaunchJump(
            true, false, true, FireworksModule.ModeChoice.TOGGLE
         )
      );
      assertFalse(
         FireworksModule.shouldRequestFastLaunchJump(
            true, true, false, FireworksModule.ModeChoice.TOGGLE
         )
      );
      assertFalse(
         FireworksModule.shouldRequestFastLaunchJump(
            true, true, true, FireworksModule.ModeChoice.MANUAL
         )
      );
      assertTrue(
         FireworksModule.shouldRequestFastLaunchJump(
            true, true, true, FireworksModule.ModeChoice.TOGGLE
         )
      );
   }
}
