package dev.schizo.client.modules.misc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AutoRespawnLifecycleTest {
   @Test
   void queuedPacketWorkRequiresSameEnabledLifecycleGeneration() {
      assertTrue(AutoRespawnModule.ownsQueuedPacketAction(true, 5L, 5L));
      assertFalse(AutoRespawnModule.ownsQueuedPacketAction(false, 5L, 5L));
      assertFalse(AutoRespawnModule.ownsQueuedPacketAction(true, 5L, 6L));
   }
}
