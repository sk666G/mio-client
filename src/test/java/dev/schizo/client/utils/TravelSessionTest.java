package dev.schizo.client.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TravelSessionTest {
   @Test
   void worldChangesAndTeleportsCannotInflateTravelDistance() {
      var session = new TravelSession();
      Object player = new Object(), level = new Object();
      session.sample(player, level, 0, 0, 0, 10);
      session.sample(player, level, 3, 4, 0, 1_000_000_010L);
      assertEquals(5, session.snapshot(1_000_000_010L).distance());
      session.sample(player, level, 1000, 4, 0, 2_000_000_010L);
      assertEquals(5, session.snapshot(2_000_000_010L).distance());
      assertEquals(1, session.snapshot(2_000_000_010L).discontinuities());
      session.sample(player, new Object(), 1000, 4, 0, 3_000_000_010L);
      assertEquals(0, session.snapshot(3_000_000_010L).distance());
      session.clear();
      assertEquals(0, session.snapshot(4_000_000_010L).seconds());
   }
}
