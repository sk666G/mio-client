package dev.schizo.client.utils.movement;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StrictStrafeSpeedHandlerTest {
   @Test
   void pausedMovementNeverRequestsTimer() {
      assertFalse(StrictStrafeSpeedHandler.shouldRequestTimer(true, true, true));
   }

   @Test
   void timerRequiresSettingAndSetbackWindow() {
      assertFalse(StrictStrafeSpeedHandler.shouldRequestTimer(false, false, true));
      assertFalse(StrictStrafeSpeedHandler.shouldRequestTimer(false, true, false));
      assertTrue(StrictStrafeSpeedHandler.shouldRequestTimer(false, true, true));
   }
}
