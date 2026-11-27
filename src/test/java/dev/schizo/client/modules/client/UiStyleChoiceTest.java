package dev.schizo.client.modules.client;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UiStyleChoiceTest {
   @Test
   void schizoStyleUsesItsOwnHeaderAndPanelDimensions() {
      UIModule.StyleChoice style = UIModule.StyleChoice.SCHIZO;

      assertEquals(21, style.getHeaderHeight());
      assertEquals(7, style.getHeightPadding());
      assertEquals(8, style.getWidthPadding());
   }

   @Test
   void existingPlainAndSlimStylesKeepTheirDimensions() {
      assertEquals(14, UIModule.StyleChoice.PLAIN.getHeaderHeight());
      assertEquals(7, UIModule.StyleChoice.PLAIN.getWidthPadding());

      assertEquals(12, UIModule.StyleChoice.SLIM.getHeaderHeight());
      assertEquals(4, UIModule.StyleChoice.SLIM.getWidthPadding());
   }
}
