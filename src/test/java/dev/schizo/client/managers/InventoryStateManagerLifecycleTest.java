package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class InventoryStateManagerLifecycleTest {
   @Test
   void deferredInventoryUpdateRequiresSamePlayerIdentity() {
      Object capturedPlayer = new Object();
      Object replacementPlayer = new Object();

      assertTrue(InventoryStateManager.matchesDeferredOwner(capturedPlayer, capturedPlayer));
      assertFalse(InventoryStateManager.matchesDeferredOwner(capturedPlayer, replacementPlayer));
      assertFalse(InventoryStateManager.matchesDeferredOwner(null, replacementPlayer));
      assertFalse(InventoryStateManager.matchesDeferredOwner(capturedPlayer, null));
   }
}
