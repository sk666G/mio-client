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
}
