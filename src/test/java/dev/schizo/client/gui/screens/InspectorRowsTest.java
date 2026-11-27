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
}
