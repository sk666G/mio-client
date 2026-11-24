package dev.schizo.client.modules.misc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StashEntryTest {
   @Test
   void sameCoordinatesAreDistinctAcrossServersAndDimensions() {
      StashEntry overworldA = new StashEntry("a", 100, -200, "OVERWORLD", "server-a");
      StashEntry overworldB = new StashEntry("b", 100, -200, "OVERWORLD", "server-b");
      StashEntry netherA = new StashEntry("c", 100, -200, "THE_NETHER", "server-a");

      assertFalse(overworldA.hasSameLocation(overworldB));
      assertFalse(overworldA.hasSameLocation(netherA));
   }

   @Test
   void identityIgnoresDescriptionButIncludesExactLocationContext() {
      StashEntry first = new StashEntry("found earlier", 100, -200, "OVERWORLD", "server-a");
      StashEntry later = new StashEntry("found later", 100, -200, "OVERWORLD", "server-a");

      assertTrue(first.hasSameLocation(later));
      assertTrue(later.hasSameLocation(first));
   }

   @Test
   void persistenceRequiresIdentityAndLocationContext() {
      assertTrue(new StashEntry("stash", 0, 0, "OVERWORLD", "singleplayer").isPersistable());
      assertFalse(new StashEntry("", 0, 0, "OVERWORLD", "server").isPersistable());
      assertFalse(new StashEntry("stash", 0, 0, null, "server").isPersistable());
      assertFalse(new StashEntry("stash", 0, 0, "OVERWORLD", null).isPersistable());
   }

   @Test
   void nullEntryNeverMatchesLocation() {
      assertFalse(new StashEntry("stash", 0, 0, "OVERWORLD", "server").hasSameLocation(null));
   }
}
