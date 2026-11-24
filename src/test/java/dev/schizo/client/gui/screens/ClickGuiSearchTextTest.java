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
}
