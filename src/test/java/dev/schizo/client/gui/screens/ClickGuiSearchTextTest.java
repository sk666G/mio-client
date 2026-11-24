package dev.schizo.client.gui.screens;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ClickGuiSearchTextTest {
   @Test
   void completesTheFullPrefixInsteadOfTheCaretPosition() {
      assertEquals("Panel", ClickGuiSearchText.completionSuffix("ExamplePanel", "Example"));
      assertEquals("plePanel", ClickGuiSearchText.completionSuffix("ExamplePanel", "Exam"));
   }

   @Test
   void preservesTheOriginalCapitalizationOfTheSuffix() {
      assertEquals("Panel", ClickGuiSearchText.completionSuffix("ExamplePanel", "EXAMPLE"));
      assertEquals("", ClickGuiSearchText.completionSuffix("ExamplePanel", "examplepanel"));
   }

   @Test
   void rejectsUnrelatedEmptyAndOverlongQueries() {
      assertNull(ClickGuiSearchText.completionSuffix("ExamplePanel", "Panel"));
      assertNull(ClickGuiSearchText.completionSuffix("ExamplePanel", ""));
      assertNull(ClickGuiSearchText.completionSuffix("ExamplePanel", "ExamplePanels"));
   }

   @Test
   void handlesSelectionInBothDirections() {
      assertEquals(new ClickGuiSearchText.Selection(2, 5, 5), ClickGuiSearchText.selection(8, 5, 2));
      assertEquals(new ClickGuiSearchText.Selection(2, 5, 2), ClickGuiSearchText.selection(8, 2, 5));
   }

   @Test
   void clampsSelectionToTheActualText() {
      assertEquals(new ClickGuiSearchText.Selection(0, 8, 8), ClickGuiSearchText.selection(8, 99, -3));
      assertEquals(new ClickGuiSearchText.Selection(0, 0, 0), ClickGuiSearchText.selection(0, 10, -1));
      assertEquals(new ClickGuiSearchText.Selection(3, 3, 3), ClickGuiSearchText.selection(8, 3, 3));
   }
}
