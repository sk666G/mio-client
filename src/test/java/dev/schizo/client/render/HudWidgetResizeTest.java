package dev.schizo.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HudWidgetResizeTest {
   @Test
   void calculatesScaleFromTheMoreRestrictiveAxis() {
      assertEquals(1.2F, HudWidgetResize.calculateScale(150.0, 120.0, 0.0, 0.0, 100.0, 100.0, 1.0F));
   }

   @Test
   void roundsDownToTwoDecimalPlaces() {
      assertEquals(1.23F, HudWidgetResize.calculateScale(123.9, 150.0, 0.0, 0.0, 100.0, 100.0, 1.0F));
   }

   @Test
   void clampsTheResultToTheSupportedWidgetScaleRange() {
      assertEquals(0.5F, HudWidgetResize.calculateScale(10.0, 10.0, 0.0, 0.0, 100.0, 100.0, 1.0F));
      assertEquals(2.0F, HudWidgetResize.calculateScale(500.0, 500.0, 0.0, 0.0, 100.0, 100.0, 1.0F));
   }

   @Test
   void preservesCurrentScaleForZeroOrNegativeDimensions() {
      assertEquals(1.25F, HudWidgetResize.calculateScale(100.0, 100.0, 0.0, 0.0, 0.0, 100.0, 1.25F));
      assertEquals(1.25F, HudWidgetResize.calculateScale(100.0, 100.0, 0.0, 0.0, 100.0, -1.0, 1.25F));
   }

   @Test
   void preservesCurrentScaleForNonFiniteInputs() {
      assertEquals(1.25F, HudWidgetResize.calculateScale(Double.NaN, 100.0, 0.0, 0.0, 100.0, 100.0, 1.25F));
      assertEquals(1.25F, HudWidgetResize.calculateScale(100.0, 100.0, 0.0, 0.0, Double.POSITIVE_INFINITY, 100.0, 1.25F));
      assertEquals(1.0F, HudWidgetResize.calculateScale(100.0, 100.0, 0.0, 0.0, 100.0, 100.0, Float.NaN));
   }

   @Test
   void handlesPointerMovementBeforeTheWidgetOrigin() {
      assertEquals(0.5F, HudWidgetResize.calculateScale(-50.0, -50.0, 0.0, 0.0, 100.0, 100.0, 1.0F));
   }
}
