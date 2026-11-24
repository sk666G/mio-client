package dev.schizo.client.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class FloatAnimationTest {
   private static final long MILLIS = 1_000_000L;

   @Test
   void interpolatesUsingTheMonotonicClock() {
      AtomicLong clock = new AtomicLong(1_000_000_000L);
      FloatAnimation animation = new FloatAnimation(clock::get);

      animation.setTarget(1.0F, 1_000L);
      assertEquals(0.0F, animation.getValue(), 0.000001F);

      clock.addAndGet(500L * MILLIS);
      assertEquals(0.5F, animation.getValue(), 0.000001F);

      clock.addAndGet(500L * MILLIS);
      assertEquals(1.0F, animation.getValue(), 0.000001F);
   }

   @Test
   void retargetsFromTheCurrentInterpolatedValue() {
      AtomicLong clock = new AtomicLong(0L);
      FloatAnimation animation = new FloatAnimation(clock::get);

      animation.setTarget(1.0F, 1_000L);
      clock.addAndGet(500L * MILLIS);
      animation.setTarget(0.0F, 500L);

      assertEquals(0.5F, animation.getValue(), 0.000001F);
      clock.addAndGet(250L * MILLIS);
      assertEquals(0.25F, animation.getValue(), 0.000001F);
      clock.addAndGet(250L * MILLIS);
      assertEquals(0.0F, animation.getValue(), 0.000001F);
   }

   @Test
   void appliesEasingToMonotonicProgress() {
      AtomicLong clock = new AtomicLong(0L);
      FloatAnimation animation = new FloatAnimation(clock::get);

      animation.setTarget(1.0F, 1_000L);
      clock.addAndGet(500L * MILLIS);

      assertEquals(0.25F, animation.getValue(value -> {
         float progress = (Float)value;
         return progress * progress;
      }), 0.000001F);
   }

   @Test
   void zeroAndNegativeDurationsCompleteImmediately() {
      AtomicLong clock = new AtomicLong(100L);
      FloatAnimation animation = new FloatAnimation(clock::get);

      animation.setTarget(1.0F, 0L);
      assertEquals(1.0F, animation.getValue(), 0.000001F);

      animation.setTarget(0.0F, -500L);
      assertEquals(0.0F, animation.getValue(), 0.000001F);
   }

   @Test
   void elapsedTimeSurvivesSignedLongWraparound() {
      AtomicLong clock = new AtomicLong(Long.MAX_VALUE - 50L * MILLIS);
      FloatAnimation animation = new FloatAnimation(clock::get);

      animation.setTarget(1.0F, 100L);
      clock.addAndGet(50L * MILLIS);
      assertEquals(0.5F, animation.getValue(), 0.000001F);

      clock.addAndGet(50L * MILLIS);
      assertEquals(1.0F, animation.getValue(), 0.000001F);
   }
}
