package dev.schizo.client.gui.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class PerformanceViewportTest {
   @Test
   void preservesNormalScaleAndFitsEachConstrainedAxis() {
      assertEquals(1.0F, PerformanceViewport.fitScale(800, 600));
      assertEquals(0.5F, PerformanceViewport.fitScale(150, 600));
      assertEquals(0.5F, PerformanceViewport.fitScale(1240, 170));
      assertEquals(1.0F, PerformanceViewport.fitScale(0, 0));
   }
}
