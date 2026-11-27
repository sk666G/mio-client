package dev.schizo.client.gui.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CommandPaletteLayoutTest {
   @Test
   void usesPreferredWidthAndMaximumRowsWhenSpaceAllows() {
      CommandPaletteLayout.Layout layout = CommandPaletteLayout.calculate(960, 540);

      assertEquals(390.0F, layout.panelWidth());
      assertEquals(285.0F, layout.panelLeft());
      assertEquals(CommandPaletteLayout.MAX_VISIBLE_RESULTS, layout.visibleRows());
      assertTrue(layout.panelTop() >= CommandPaletteLayout.TOP_MARGIN);
   }

   @Test
   void fitsNarrowViewportsInsideHorizontalMargins() {
      CommandPaletteLayout.Layout layout = CommandPaletteLayout.calculate(200, 540);

      assertEquals(176.0F, layout.panelWidth());
      assertEquals(12.0F, layout.panelLeft());
      assertEquals(CommandPaletteLayout.MAX_VISIBLE_RESULTS, layout.visibleRows());
   }

   @Test
   void reducesVisibleRowsToFitShortViewports() {
      CommandPaletteLayout.Layout layout = CommandPaletteLayout.calculate(400, 180);

      assertEquals(3, layout.visibleRows());
      float panelHeight = CommandPaletteLayout.HEADER_HEIGHT
         + layout.visibleRows() * CommandPaletteLayout.RESULT_ROW_HEIGHT
         + CommandPaletteLayout.FOOTER_HEIGHT;

      assertTrue(layout.panelTop() >= CommandPaletteLayout.TOP_MARGIN);
      assertTrue(layout.panelTop() + panelHeight <= 180 - CommandPaletteLayout.TOP_MARGIN);
   }

   @Test
   void neverReturnsNegativeGeometryForTinyViewports() {
      CommandPaletteLayout.Layout layout = CommandPaletteLayout.calculate(1, 1);

      assertTrue(layout.panelLeft() >= 0.0F);
      assertTrue(layout.panelTop() >= 0.0F);
      assertTrue(layout.panelWidth() >= 1.0F);
      assertTrue(layout.visibleRows() >= 1);
   }
}
