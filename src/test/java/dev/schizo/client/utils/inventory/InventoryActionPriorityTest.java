package dev.schizo.client.utils.inventory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InventoryActionPriorityTest {
   @Test
   void protectionRequiresAnEnabledConfiguredTotemAndDoesNotBlockOffhandFood() {
      assertTrue(InventoryActionPriority.protectMainhand(true, true, true, false, 8, 8));
      assertTrue(InventoryActionPriority.protectMainhand(true, true, true, true, 20, 8));
      assertFalse(InventoryActionPriority.protectMainhand(false, true, true, true, 20, 8));
      assertFalse(InventoryActionPriority.protectMainhand(true, true, false, true, 20, 8));
      assertFalse(InventoryActionPriority.protectMainhand(true, true, true, false, 20, 8));
      assertTrue(InventoryActionPriority.pauseFood(true, false));
      assertFalse(InventoryActionPriority.pauseFood(true, true));
   }
}
