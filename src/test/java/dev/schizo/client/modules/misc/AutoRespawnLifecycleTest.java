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

   @Test
   void queuedPacketWorkCannotCrossConnectionOrLifecycle() {
      Object connection = new Object();

      assertTrue(AutoRespawnModule.ownsQueuedPacketSession(true, 9L, 9L, connection, connection));
      assertFalse(AutoRespawnModule.ownsQueuedPacketSession(false, 9L, 9L, connection, connection));
      assertFalse(AutoRespawnModule.ownsQueuedPacketSession(true, 9L, 10L, connection, connection));
      assertFalse(AutoRespawnModule.ownsQueuedPacketSession(true, 9L, 9L, connection, new Object()));
      assertFalse(AutoRespawnModule.ownsQueuedPacketSession(true, 9L, 9L, null, null));
      assertFalse(AutoRespawnModule.ownsQueuedPacketSession(true, 9L, 9L, null, connection));
   }
}
