package dev.schizo.client.render;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudWidgetScaleStepTest {
   @Test void percentStepsAreStableAndBounded() {
      assertEquals(1.01F, HudWidgetResize.adjustPercent(1.0F, 1));
      assertEquals(0.99F, HudWidgetResize.adjustPercent(1.0F, -1));
      assertEquals(1.1F, HudWidgetResize.adjustPercent(1.0F, 10));
      assertEquals(2.0F, HudWidgetResize.adjustPercent(1.99F, 10));
      assertEquals(0.5F, HudWidgetResize.adjustPercent(0.51F, -10));
      assertEquals(2.0F, HudWidgetResize.adjustPercent(1.0F, Integer.MAX_VALUE));
      assertEquals(0.5F, HudWidgetResize.adjustPercent(1.0F, Integer.MIN_VALUE));
      assertEquals(1.0F, HudWidgetResize.adjustPercent(Float.NaN, 1));
   }
}
