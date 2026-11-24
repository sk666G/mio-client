package dev.schizo.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HudWidgetDragTest {
   @Test
   void preservesFractionalGrabOffsets() {
      assertEquals(100.25F, HudWidgetDrag.clampCoordinate(125.75, 25.5, 800.0F, 100.0F), 0.000001F);
   }

   @Test
   void clampsBothViewportEdges() {
      assertEquals(0.0F, HudWidgetDrag.clampCoordinate(10.0, 30.0, 800.0F, 100.0F));
      assertEquals(700.0F, HudWidgetDrag.clampCoordinate(900.0, 25.0, 800.0F, 100.0F));
   }

   @Test
   void pinsOversizedWidgetsToTheLeadingEdge() {
      assertEquals(0.0F, HudWidgetDrag.clampCoordinate(60.0, 10.0, 80.0F, 120.0F));
   }

   @Test
   void rejectsNonFinitePointerInputs() {
      assertEquals(0.0F, HudWidgetDrag.clampCoordinate(Double.NaN, 1.0, 800.0F, 100.0F));
      assertEquals(0.0F, HudWidgetDrag.clampCoordinate(50.0, Double.POSITIVE_INFINITY, 800.0F, 100.0F));
   }

   @Test
   void saturatesFiniteDoubleCoordinatesBeforeViewportClamping() {
      assertEquals(700.0F, HudWidgetDrag.clampCoordinate(Double.MAX_VALUE, 0.0, 800.0F, 100.0F));
      assertEquals(0.0F, HudWidgetDrag.clampCoordinate(-Double.MAX_VALUE, 0.0, 800.0F, 100.0F));
   }
}
