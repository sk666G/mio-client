package dev.schizo.client.gui.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ScreenTooltipLayoutTest {
   @Test
   void keepsTooltipPositionWhenItAlreadyFits() {
      assertEquals(40.0F, ScreenTooltipLayout.clampStart(40.0F, 80.0F, 200.0F), 0.0001F);
   }

   @Test
   void clampsTooltipAgainstBothViewportEdges() {
      assertEquals(0.0F, ScreenTooltipLayout.clampStart(-12.0F, 80.0F, 200.0F), 0.0001F);
      assertEquals(120.0F, ScreenTooltipLayout.clampStart(180.0F, 80.0F, 200.0F), 0.0001F);
   }

   @Test
   void anchorsOversizedTooltipAtViewportOrigin() {
      assertEquals(0.0F, ScreenTooltipLayout.clampStart(50.0F, 240.0F, 200.0F), 0.0001F);
   }

   @Test
   void preservesPositionForInvalidGeometry() {
      assertEquals(25.0F, ScreenTooltipLayout.clampStart(25.0F, 0.0F, 200.0F), 0.0001F);
      assertEquals(25.0F, ScreenTooltipLayout.clampStart(25.0F, 80.0F, 0.0F), 0.0001F);
   }

   @Test
   void wrapsWordsAndPreservesExplicitParagraphs() {
      var layout=ScreenTooltipLayout.wrap("alpha beta\n\ngamma",5,8,String::length);
      assertEquals(java.util.List.of("alpha","beta","","gamma"),layout.lines());
      assertEquals(5F,layout.width());
   }

   @Test
   void truncatesToViewportAndDoesNotSplitSurrogatePairs() {
      var layout=ScreenTooltipLayout.wrap("😀😀😀😀",2,2,String::length);
      assertEquals(java.util.List.of("😀","…"),layout.lines());
      assertEquals(2F,layout.width());
      var tiny=ScreenTooltipLayout.wrap("hello",0,2,String::length);
      assertEquals(java.util.List.of(),tiny.lines());
   }

   @Test
   void longUnbrokenWordsAndLargeInputsAreBounded() {
      var layout=ScreenTooltipLayout.wrap("a".repeat(10000),10,100,String::length);
      assertEquals(24,layout.lines().size());
      org.junit.jupiter.api.Assertions.assertTrue(layout.lines().getLast().endsWith("…"));
      org.junit.jupiter.api.Assertions.assertTrue(layout.lines().stream().allMatch(line->line.length()<=10));
   }
}
