package dev.schizo.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.schizo.client.utils.ClampedInterpolatedFloat;
import dev.schizo.client.utils.FloatAnimation;
import dev.schizo.client.utils.InterpolatedFloat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class UiMotionPolicyTest {
   @AfterEach
   void restoreMotion() {
      UiMotionPolicy.setReducedMotion(false);
   }

   @Test
   void reducedMotionSnapsTimedAnimationToTarget() {
      UiMotionPolicy.setReducedMotion(true);
      FloatAnimation animation = new FloatAnimation();
      animation.setValue(0.0F);
      animation.setTarget(1.0F, 5_000L);

      assertEquals(1.0F, animation.getValue());
   }

   @Test
   void reducedMotionSnapsInterpolatedValuesWithoutFrameTiming() {
      UiMotionPolicy.setReducedMotion(true);

      InterpolatedFloat interpolated = new InterpolatedFloat(1.0F, true);
      interpolated.setValue(0.0F);
      interpolated.setTarget(10.0F);
      assertEquals(10.0F, interpolated.updateAndGet());

      ClampedInterpolatedFloat clamped = new ClampedInterpolatedFloat(1.0F, true);
      clamped.setValue(0.0F);
      clamped.setTarget(true);
      assertEquals(1.0F, clamped.updateAndGet());
   }
}
