package dev.schizo.client.gui.panels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class PanelBoundsTest {
   @Test
   void keepsTheHeaderReachableWhenThePanelExceedsTheWindow() {
      assertEquals(0, PanelBounds.clampPosition(54, 80, 120));
   }

   @Test
   void clampsSavedAndDraggedPositionsAtFractionalGuiScale() {
      assertEquals(12, PanelBounds.clampPosition(Integer.MAX_VALUE, 112.75, 100));
      assertEquals(0, PanelBounds.clampPosition(Integer.MIN_VALUE, 112.75, 100));
      assertEquals(7, PanelBounds.clampPosition(7, 112.75, 100));
   }   @Test
   void heightLimitNeverShrinksBelowThePanelHeader() {
      assertEquals(13, PanelBounds.ensureHeaderVisible(-40, 12));
      assertEquals(13, PanelBounds.ensureHeaderVisible(5, 12));
      assertEquals(90, PanelBounds.ensureHeaderVisible(90, 12));
   }


}
