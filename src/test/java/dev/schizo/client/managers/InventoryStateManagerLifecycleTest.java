package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class InventoryStateManagerLifecycleTest {
   @Test
   void deferredInventoryUpdateRequiresSamePlayerAndContainerIdentity() {
      Object capturedPlayer = new Object();
      Object replacementPlayer = new Object();

      assertTrue(InventoryStateManager.matchesDeferredContext(capturedPlayer, capturedPlayer, 4, 4));
      assertFalse(InventoryStateManager.matchesDeferredContext(capturedPlayer, replacementPlayer, 4, 4));
      assertFalse(InventoryStateManager.matchesDeferredContext(capturedPlayer, capturedPlayer, 4, 7));
      assertFalse(InventoryStateManager.matchesDeferredContext(null, replacementPlayer, 4, 4));
      assertFalse(InventoryStateManager.matchesDeferredContext(capturedPlayer, null, 4, 4));
   }
}
