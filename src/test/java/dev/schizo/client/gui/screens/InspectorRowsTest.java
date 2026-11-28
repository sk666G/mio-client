package dev.schizo.client.gui.screens;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InspectorRowsTest {
   @Test void hitRespectsViewportGapsAndLastItem() {
      assertEquals(3, InspectorRows.hit(20, 101, 10, 100, 200, 3, 3, 5));
      assertEquals(4, InspectorRows.hit(20, 114, 10, 100, 200, 3, 3, 5));
      assertEquals(-1, InspectorRows.hit(20, 112, 10, 100, 200, 3, 3, 5));
      assertEquals(-1, InspectorRows.hit(20, 127, 10, 100, 200, 3, 3, 5));
      assertEquals(-1, InspectorRows.hit(210, 101, 10, 100, 200, 3, 3, 5));
   }

   @Test void revealKeepsSelectedRowVisibleWithoutUnnecessaryScroll() {
      assertEquals(2, InspectorRows.reveal(3, 2, 4, 12));
      assertEquals(5, InspectorRows.reveal(8, 2, 4, 12));
      assertEquals(1, InspectorRows.reveal(1, 5, 4, 12));
      assertEquals(0, InspectorRows.reveal(0, 5, 4, 0));
   }

   @Test void stepClampsSingleRowAndPageNavigation() {
      assertEquals(0, InspectorRows.step(0, -1, 12));
      assertEquals(11, InspectorRows.step(11, 1, 12));
      assertEquals(7, InspectorRows.step(3, 4, 12));
      assertEquals(0, InspectorRows.step(3, -4, 12));
      assertEquals(11, InspectorRows.step(9, 4, 12));
      assertEquals(0, InspectorRows.step(0, 5, 1));
   }

   @Test void stepHandlesEmptyListsStaleIndicesAndLargeDeltas() {
      assertEquals(-1, InspectorRows.step(3, 1, 0));
      assertEquals(1, InspectorRows.step(-1, 1, 12));
      assertEquals(10, InspectorRows.step(99, -1, 12));
      assertEquals(11, InspectorRows.step(10, Integer.MAX_VALUE, 12));
      assertEquals(0, InspectorRows.step(10, Integer.MIN_VALUE, 12));
   }
}
