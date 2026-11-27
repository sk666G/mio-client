package dev.schizo.client.render;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LoadingMotionTest {
   @Test void progressNeverExceedsRail() {
      assertEquals(0, LoadingMotion.progressWidth(240, -1));
      assertEquals(120, LoadingMotion.progressWidth(240, 0.5F));
      assertEquals(240, LoadingMotion.progressWidth(240, 1.01F));
      assertEquals(0, LoadingMotion.progressWidth(240, Float.NaN));
      assertEquals(0, LoadingMotion.progressWidth(-5, 1));
   }

   @Test void followsActualOverlayFadeAndReducedMotion() {
      assertEquals(1F, LoadingMotion.opacity(0, -1, -1));
      assertEquals(0F, LoadingMotion.opacity(100, 100, -1));
      assertEquals(0.5F, LoadingMotion.opacity(350, 100, -1));
      assertEquals(1F, LoadingMotion.opacity(1100, 100, 100));
      assertEquals(0.5F, LoadingMotion.opacity(1600, 100, 100));
      assertEquals(0F, LoadingMotion.opacity(2100, 100, 100));
      assertEquals(0.75F, LoadingMotion.logoScale(0, true));
      assertEquals(0.75F, LoadingMotion.logoScale(1, false));
      assertTrue(LoadingMotion.logoScale(0, false) < LoadingMotion.logoScale(1, false));
   }
}
