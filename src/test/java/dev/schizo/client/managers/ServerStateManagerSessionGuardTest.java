package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ServerStateManagerSessionGuardTest {
   @Test
   void queuedSessionRequiresSameGenerationAndPlayerIdentity() {
      Object player = new Object();

      assertTrue(ServerStateManager.matchesQueuedSession(4L, 4L, player, player));
      assertFalse(ServerStateManager.matchesQueuedSession(4L, 5L, player, player));
      assertFalse(ServerStateManager.matchesQueuedSession(4L, 4L, player, new Object()));
      assertFalse(ServerStateManager.matchesQueuedSession(4L, 4L, null, null));
   }
}
