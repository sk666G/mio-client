package dev.schizo.client.gui.screens;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StudioTextSelectionTest {
   @Test void preservesSelectionDirectionAndCollapsedCaret() {
      var forward = new StudioTextSelection("abcdef",5,1);
      var backward = new StudioTextSelection("abcdef",1,5);
      assertEquals(5,forward.caret());
      assertEquals(1,forward.anchor());
      assertEquals(1,backward.caret());
      assertEquals(5,backward.anchor());
      assertEquals(3,new StudioTextSelection("abcdef",3,3).anchor());
   }

   @Test void boundsEndpointsAndAvoidsSplittingSurrogates() {
      var selection = new StudioTextSelection("a\uD83D\uDE00b",2,99);
      assertEquals(1,selection.caret());
      assertEquals(4,selection.anchor());
      assertEquals(0,new StudioTextSelection("",-5,99).anchor());
   }

   @Test void refusesSelectionTransferOntoDifferentText() {
      var selection = new StudioTextSelection("123",1,2);
      assertTrue(selection.matches(new String("123")));
      assertFalse(selection.matches("456"));
      assertFalse(selection.matches("12"));
      assertFalse(selection.matches(null));
   }
}
