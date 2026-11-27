package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class WorldStateManagerPacketContextTest {
   @Test
   void worldPacketUpdateRequiresSameGenerationAndLevelIdentity() {
      Object level = new Object();

      assertTrue(WorldStateManager.matchesWorldPacketContext(3L, 3L, level, level));
      assertFalse(WorldStateManager.matchesWorldPacketContext(3L, 4L, level, level));
      assertFalse(WorldStateManager.matchesWorldPacketContext(3L, 3L, level, new Object()));
      assertFalse(WorldStateManager.matchesWorldPacketContext(3L, 3L, null, null));
   }
}
