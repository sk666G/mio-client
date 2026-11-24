package dev.schizo.client.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.schizo.client.ClientContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InterpolatedFloatLowFpsTest {
   private FrameTiming previousFrameTiming;
   private FrameTiming timing;
   private long nowNanos;

   @BeforeEach
   void setUp() {
      this.previousFrameTiming = ClientContext.frameTiming;
      this.timing = new FrameTiming();
      ClientContext.frameTiming = this.timing;
      this.nowNanos = 1_000_000_000L;
      this.timing.recordFrameAt(this.nowNanos);
      this.advanceFrame(100_000_000L);
   }

   @AfterEach
   void tearDown() {
      ClientContext.frameTiming = this.previousFrameTiming;
   }

   @Test
   void ordinaryInterpolationDoesNotReboundAfterLowFpsSnap() {
      InterpolatedFloat value = new InterpolatedFloat(1.0F, false);
      value.setTarget(10.0F);

      assertEquals(10.0F, value.updateAndGet(), 0.000001F);
      this.raiseFpsAboveSnapThreshold();

      assertTrue(this.timing.getFramesPerSecond() > 15);
      assertEquals(10.0F, value.updateAndGet(), 0.000001F);
   }

   @Test
   void clampedInterpolationDoesNotReboundAfterLowFpsSnap() {
      ClampedInterpolatedFloat value = new ClampedInterpolatedFloat(1.0F, false);
      value.setTarget(1.0F);

      assertEquals(1.0F, value.updateAndGet(), 0.000001F);
      this.raiseFpsAboveSnapThreshold();

      assertTrue(this.timing.getFramesPerSecond() > 15);
      assertEquals(1.0F, value.updateAndGet(), 0.000001F);
   }

   private void raiseFpsAboveSnapThreshold() {
      for (int index = 0; index < 20; index++) {
         this.advanceFrame(16_000_000L);
      }
   }

   private void advanceFrame(long elapsedNanos) {
      this.nowNanos += elapsedNanos;
      this.timing.recordFrameAt(this.nowNanos);
   }
}
