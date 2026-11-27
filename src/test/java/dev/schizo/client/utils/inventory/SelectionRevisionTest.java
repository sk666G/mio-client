package dev.schizo.client.utils.inventory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SelectionRevisionTest {
   @Test
   void returningToTheSameSlotDoesNotReviveAnOldLease() {
      var state = new SelectionRevision();
      state.changed(); // module A selects slot 2
      long firstLease = state.current();
      state.changed(); // user selects slot 4
      state.changed(); // module B selects slot 2
      assertFalse(state.owns(firstLease));
      assertTrue(state.owns(state.current()));
   }
}
