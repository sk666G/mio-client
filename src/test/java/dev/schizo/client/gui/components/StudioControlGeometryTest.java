package dev.schizo.client.gui.components;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StudioControlGeometryTest {
   @Test void sliderProgressIsBoundedAndFinite() {
      assertEquals(0.5F, StudioControlGeometry.fraction(5, 0, 10));
      assertEquals(0.0F, StudioControlGeometry.fraction(-10, 0, 10));
      assertEquals(1.0F, StudioControlGeometry.fraction(20, 0, 10));
      assertEquals(0.0F, StudioControlGeometry.fraction(5, 5, 5));
      assertEquals(0.0F, StudioControlGeometry.fraction(Double.NaN, 0, 10));
      assertEquals(0.0F, StudioControlGeometry.fraction(5, 10, 0));
   }

   @Test void thumbRemainsInsideTrackDuringTransition() {
      assertEquals(10.0F, StudioControlGeometry.thumbLeft(10, 24, 8, -1));
      assertEquals(18.0F, StudioControlGeometry.thumbLeft(10, 24, 8, 0.5F));
      assertEquals(26.0F, StudioControlGeometry.thumbLeft(10, 24, 8, 2));
      assertEquals(10.0F, StudioControlGeometry.thumbLeft(10, 24, 8, Float.NaN));
   }
}
