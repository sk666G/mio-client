package dev.schizo.client.modules.misc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AntiAFKModuleSessionGuardTest {
   @Test
   void queuedReplyRequiresSameLifecyclePlayerAndConnection() {
      Object player = new Object();
      Object connection = new Object();

      assertTrue(AntiAFKModule.matchesReplyContext(7L, 7L, player, player, connection, connection));
      assertFalse(AntiAFKModule.matchesReplyContext(7L, 8L, player, player, connection, connection));
      assertFalse(AntiAFKModule.matchesReplyContext(7L, 7L, player, new Object(), connection, connection));
      assertFalse(AntiAFKModule.matchesReplyContext(7L, 7L, player, player, connection, new Object()));
      assertFalse(AntiAFKModule.matchesReplyContext(7L, 7L, null, null, null, null));
   }
}
