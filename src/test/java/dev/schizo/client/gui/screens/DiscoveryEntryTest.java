package dev.schizo.client.gui.screens;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DiscoveryEntryTest {
   @Test
   void filtersByServerBeforeSearchingAndPreservesUnknownAltitude() {
      var row = new DiscoveryEntry("Stash", "Storage", "server:25565", "THE_NETHER", 1, null, 2, 0, 0);
      assertTrue(row.matches("storage", "SERVER:25565", true));
      assertFalse(row.matches("storage", "other:25565", true));
      assertTrue(row.matches("nether", "other:25565", false));
      assertTrue(row.coordinates().contains("?"));
      assertEquals("the_nether", DiscoveryEntry.canonicalDimension("minecraft:the_nether"));
      assertEquals("the_nether", DiscoveryEntry.canonicalDimension(row.dimension()));
   }
}
