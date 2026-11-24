package dev.schizo.client.render;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HudWidgetSnapTest {
   @Test
   void resolvesCenterBeforeOtherTargets() {
      assertEquals(HudWidgetSnap.Target.CENTER, HudWidgetSnap.resolve(45.0F, 10.0F, 100.0F, 45.0F, 5.0F));
      assertEquals(45.0F, HudWidgetSnap.snap(47.0F, 10.0F, 100.0F, 0.0F, 5.0F));
   }

   @Test
   void snapsLeadingAndTrailingEdges() {
      assertEquals(HudWidgetSnap.Target.LEADING, HudWidgetSnap.resolve(7.0F, 10.0F, 100.0F, 5.0F, 3.0F));
      assertEquals(5.0F, HudWidgetSnap.snap(7.0F, 10.0F, 100.0F, 5.0F, 3.0F));
      assertEquals(HudWidgetSnap.Target.TRAILING, HudWidgetSnap.resolve(83.0F, 10.0F, 100.0F, 5.0F, 3.0F));
      assertEquals(85.0F, HudWidgetSnap.snap(83.0F, 10.0F, 100.0F, 5.0F, 3.0F));
   }

   @Test
   void leavesCoordinatesOutsideTheThresholdUntouched() {
      assertEquals(HudWidgetSnap.Target.NONE, HudWidgetSnap.resolve(20.0F, 10.0F, 100.0F, 5.0F, 3.0F));
      assertEquals(20.0F, HudWidgetSnap.snap(20.0F, 10.0F, 100.0F, 5.0F, 3.0F));
   }

   @Test
   void acceptsTheExactThresholdBoundary() {
      assertEquals(HudWidgetSnap.Target.LEADING, HudWidgetSnap.resolve(8.0F, 10.0F, 100.0F, 5.0F, 3.0F));
      assertEquals(HudWidgetSnap.Target.TRAILING, HudWidgetSnap.resolve(82.0F, 10.0F, 100.0F, 5.0F, 3.0F));
   }

   @Test
   void rejectsInvalidGeometryWithoutChangingThePosition() {
      assertEquals(HudWidgetSnap.Target.NONE, HudWidgetSnap.resolve(Float.NaN, 10.0F, 100.0F, 5.0F, 3.0F));
      assertEquals(HudWidgetSnap.Target.NONE, HudWidgetSnap.resolve(10.0F, -1.0F, 100.0F, 5.0F, 3.0F));
      assertEquals(HudWidgetSnap.Target.NONE, HudWidgetSnap.resolve(10.0F, 5.0F, 0.0F, 5.0F, 3.0F));
      assertEquals(10.0F, HudWidgetSnap.snap(10.0F, 5.0F, 100.0F, 5.0F, Float.NaN));
   }
}
