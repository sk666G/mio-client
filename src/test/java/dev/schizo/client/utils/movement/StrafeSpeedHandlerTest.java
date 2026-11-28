package dev.schizo.client.utils.movement;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StrafeSpeedHandlerTest {
   @Test
   void pausedMovementNeverRequestsTimer() {
      assertFalse(StrafeSpeedHandler.shouldRequestTimer(true, true, true));
   }

   @Test
   void timerRequiresSettingAndSetbackWindow() {
      assertFalse(StrafeSpeedHandler.shouldRequestTimer(false, false, true));
      assertFalse(StrafeSpeedHandler.shouldRequestTimer(false, true, false));
      assertTrue(StrafeSpeedHandler.shouldRequestTimer(false, true, true));
   }
}
