package dev.schizo.client.gui.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
