package dev.schizo.client.gui.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class PaletteMatcherTest {
   @Test
   void ranksExactPrefixAndPartialMatchesInOrder() {
      assertEquals(0, PaletteMatcher.score("Metrics", "", "metrics"));
      assertEquals(1, PaletteMatcher.score("Metrics HUD", "", "met"));
      assertEquals(2, PaletteMatcher.score("Frame Metrics", "", "metrics"));
      assertEquals(3, PaletteMatcher.score("One Percent Low", "Metrics HUD setting", "metrics"));
   }

   @Test
   void supportsFuzzySubsequenceMatches() {
      assertEquals(4, PaletteMatcher.score("AutoCrystal", "", "atcr"));
      assertEquals(-1, PaletteMatcher.score("AutoCrystal", "", "zzzz"));
   }

   @Test
   void treatsBlankQueriesAsUnfilteredAndIgnoresCase() {
      assertEquals(0, PaletteMatcher.score("Anything", "", "   "));
      assertEquals(0, PaletteMatcher.score("Command", "", "COMMAND"));
   }

   @Test
   void rejectsNullQueryAsAnEmptySearch() {
      assertEquals(0, PaletteMatcher.score("Anything", "", null));
   }

   @Test
   void supportsCrossFieldTermsWithoutRequiringOrder() {
      assertEquals(4, PaletteMatcher.score("Size", "Crosshair setting", "crosshair size"));
      assertEquals(4, PaletteMatcher.score("Size", "Crosshair setting", "size crosshair"));
      assertEquals(-1, PaletteMatcher.score("Size", "Crosshair setting", "crosshair unknown"));
   }

   @Test
   void acceptsSingleTypoOnlyAsLowestPriorityFallback() {
      assertEquals(5, PaletteMatcher.score("Metrics", "", "metrcis"));
      assertEquals(5, PaletteMatcher.score("Metrics", "", "metrixs"));
      assertEquals(5, PaletteMatcher.score("Frame Metrics", "", "metrisc"));
      assertEquals(5, PaletteMatcher.score("Metrics", "", "mettrics"));
      assertEquals(4, PaletteMatcher.score("Metrics", "", "metrcs"));
      assertEquals(-1, PaletteMatcher.score("HUD", "", "HXD"));
      assertEquals(-1, PaletteMatcher.score("Metrics", "", "mexxics"));
   }

   @Test
   void moduleFilteringKeepsShortQueriesLiteralAndSupportsLongerFuzzyMatches() {
      assertTrue(PaletteMatcher.matchesModule("Crosshair", "render length gap", "crossahir"));
      assertTrue(PaletteMatcher.matchesModule("Crosshair", "render length gap", "length crosshair"));
      assertTrue(PaletteMatcher.matchesModule("Crosshair", "render length gap", "gap"));
      assertTrue(PaletteMatcher.matchesModule("Crosshair", "render length gap", "  CROSSHAIR "));
      assertFalse(PaletteMatcher.matchesModule("Crosshair", "render length gap", "crh"));
      assertFalse(PaletteMatcher.matchesModule("Crosshair", "render length gap", "crosshair unknown"));
   }
}
