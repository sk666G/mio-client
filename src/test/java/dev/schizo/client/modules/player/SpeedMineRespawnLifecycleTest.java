package dev.schizo.client.modules.player;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SpeedMineRespawnLifecycleTest {
   @Test
   void queuedRespawnResetRequiresSameEnabledLifecycleGeneration() {
      assertTrue(SpeedMineModule.ownsQueuedRespawnReset(true, 11L, 11L));
      assertFalse(SpeedMineModule.ownsQueuedRespawnReset(false, 11L, 11L));
      assertFalse(SpeedMineModule.ownsQueuedRespawnReset(true, 11L, 12L));
   }
}
