package dev.schizo.client.modules.misc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AutoRespawnModuleSessionGuardTest {
   @Test
   void queuedRespawnAndKillCallbacksRequireOriginalEnabledSession() {
      Object connection = new Object();

      assertTrue(AutoRespawnModule.matchesRespawnSession(true, 7L, 7L, connection, connection));
      assertFalse(AutoRespawnModule.matchesRespawnSession(false, 7L, 7L, connection, connection));
      assertFalse(AutoRespawnModule.matchesRespawnSession(true, 7L, 8L, connection, connection));
      assertFalse(AutoRespawnModule.matchesRespawnSession(true, 7L, 7L, connection, new Object()));
      assertFalse(AutoRespawnModule.matchesRespawnSession(true, 7L, 7L, null, null));
      assertFalse(AutoRespawnModule.matchesRespawnSession(true, 7L, 7L, null, connection));
   }
}
