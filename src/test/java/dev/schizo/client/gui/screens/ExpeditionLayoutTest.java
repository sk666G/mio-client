package dev.schizo.client.gui.screens;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ExpeditionLayoutTest {
   @Test
   void fullViewportPreservesExistingGeometry() {
      var layout = ExpeditionLayout.forViewportHeight(480);
      assertEquals(10, layout.titleY());
      assertEquals(27, layout.searchY());
      assertEquals(49, layout.tabsY());
      assertEquals(72, layout.secondaryY());
      assertEquals(100, layout.rowsTop());
      assertEquals(416, layout.rowsBottom());
      assertEquals(9, layout.visibleRows());
      assertTrue(layout.showSecondaryControls());
      assertTrue(layout.showActions());
      assertTrue(layout.showPagination());
   }

   @Test
   void shortViewportCollapsesSecondaryAndPaginationRows() {
      var layout = ExpeditionLayout.forViewportHeight(160);
      assertFalse(layout.showSecondaryControls());
      assertFalse(layout.showPagination());
      assertTrue(layout.showActions());
      assertTrue(layout.rowsTop() < layout.rowsBottom());
      assertTrue(layout.rowsBottom() <= layout.actionY());
      assertEquals(1, layout.visibleRows());
   }

   @Test
   void veryShortViewportHidesActionsBeforeTheyOverlapContent() {
      var layout = ExpeditionLayout.forViewportHeight(120);
      assertFalse(layout.showActions());
      assertFalse(layout.showSecondaryControls());
      assertFalse(layout.showPagination());
      assertTrue(layout.rowsBottom() <= layout.statusY());
      assertEquals(1, layout.visibleRows());
   }

   @Test
   void tinyViewportNeverProducesNegativeGeometry() {
      var layout = ExpeditionLayout.forViewportHeight(40);
      assertEquals(0, layout.visibleRows());
      assertTrue(layout.rowsTop() >= 0);
      assertTrue(layout.rowsBottom() >= layout.rowsTop());
      assertTrue(layout.statusY() >= 0);
   }
}
