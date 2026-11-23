package dev.schizo.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HudWidgetPositionTest {
   @Test
   void preservesPositionsThatAlreadyFit() {
      assertEquals(120.0F, HudWidgetPosition.clampToViewport(120.0F, 800.0F, 100.0F));
   }

   @Test
   void clampsBothViewportEdges() {
      assertEquals(0.0F, HudWidgetPosition.clampToViewport(-25.0F, 800.0F, 100.0F));
      assertEquals(700.0F, HudWidgetPosition.clampToViewport(950.0F, 800.0F, 100.0F));
   }

   @Test
   void pinsAnOversizedWidgetToTheLeadingEdge() {
      assertEquals(0.0F, HudWidgetPosition.clampToViewport(30.0F, 80.0F, 120.0F));
   }

   @Test
   void normalizesTheClampedCoordinate() {
      assertEquals(0.875F, HudWidgetPosition.toNormalizedPosition(950.0F, 800.0F, 100.0F), 0.000001F);
   }

   @Test
   void safelyHandlesInvalidViewportAndNanPositions() {
      assertEquals(0.0F, HudWidgetPosition.clampToViewport(10.0F, 0.0F, 5.0F));
      assertEquals(0.0F, HudWidgetPosition.clampToViewport(Float.NaN, 800.0F, 100.0F));
      assertEquals(0.0F, HudWidgetPosition.toNormalizedPosition(10.0F, Float.NaN, 5.0F));
   }

   @Test
   void treatsInvalidWidgetDimensionsAsViewportSized() {
      assertEquals(0.0F, HudWidgetPosition.clampToViewport(100.0F, 800.0F, Float.NaN));
   }
}
