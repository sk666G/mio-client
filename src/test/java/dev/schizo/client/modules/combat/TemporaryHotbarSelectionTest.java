package dev.schizo.client.modules.combat;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TemporaryHotbarSelectionTest {
   @Test
   void restoresPreviousSlotWhenOwnedSelectionIsStillActive() {
      TemporaryHotbarSelection selection = new TemporaryHotbarSelection();
      selection.acquire(2, 5);

      assertEquals(2, selection.release(5));
      assertFalse(selection.isActive());
   }

   @Test
   void doesNotOverwriteASelectionChangedByAnotherOwner() {
      TemporaryHotbarSelection selection = new TemporaryHotbarSelection();
      selection.acquire(2, 5);

      assertEquals(-1, selection.release(7));
      assertFalse(selection.isActive());
   }

   @Test
   void clearDropsOwnershipWithoutRestore() {
      TemporaryHotbarSelection selection = new TemporaryHotbarSelection();
      selection.acquire(1, 4);

      selection.clear();

      assertFalse(selection.isActive());
      assertEquals(-1, selection.release(4));
   }
}
