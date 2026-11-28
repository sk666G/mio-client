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

   @Test
   void setValueSnapsEvenWhenTheRequestedValueIsAlreadyTheActiveTarget() {
      AtomicLong clock = new AtomicLong(0L);
      FloatAnimation animation = new FloatAnimation(clock::get);

      animation.setValue(1.0F);
      animation.setTarget(0.0F, 1_000L);
      clock.addAndGet(500L * MILLIS);
      assertEquals(0.5F, animation.getValue(), 0.000001F);

      animation.setValue(0.0F);
      assertEquals(0.0F, animation.getValue(), 0.000001F);

      clock.addAndGet(500L * MILLIS);
      assertEquals(0.0F, animation.getValue(), 0.000001F);
   }

   @Test
   void setValueCanRestartAnAnimationFromAnExactBaseline() {
      AtomicLong clock = new AtomicLong(0L);
      FloatAnimation animation = new FloatAnimation(clock::get);

      animation.setTarget(1.0F, 1_000L);
      clock.addAndGet(600L * MILLIS);
      assertEquals(0.6F, animation.getValue(), 0.000001F);

      animation.setValue(0.0F);
      animation.setTarget(1.0F, 200L);
      assertEquals(0.0F, animation.getValue(), 0.000001F);

      clock.addAndGet(100L * MILLIS);
      assertEquals(0.5F, animation.getValue(), 0.000001F);
   }

   @Test
   void completedAnimationReturnsExactTargetWithoutFurtherClockReads() {
      AtomicLong clock=new AtomicLong();
      AtomicLong reads=new AtomicLong();
      FloatAnimation animation=new FloatAnimation(()->{reads.incrementAndGet();return clock.get();});
      animation.setValue(1.0e20F);
      animation.setTarget(1.0F,100L);
      clock.set(100L*MILLIS);
      assertEquals(1.0F,animation.getValue());
      long settledReads=reads.get();
      for(int i=0;i<100;i++) assertEquals(1.0F,animation.getValue());
      assertEquals(1.0F,animation.getValue(value->{throw new AssertionError("settled easing invoked");}));
      assertEquals(settledReads,reads.get());
   }

   @Test
   void reducedMotionReadFinishesInFlightAnimationWithoutRestartingIt() {
      boolean previous=dev.schizo.client.gui.UiMotionPolicy.isReducedMotion();
      try {
         dev.schizo.client.gui.UiMotionPolicy.setReducedMotion(false);
         AtomicLong clock=new AtomicLong();
         FloatAnimation animation=new FloatAnimation(clock::get);
         animation.setTarget(1.0F,1000L);
         clock.set(250L*MILLIS);
         assertEquals(0.25F,animation.getValue(),0.000001F);
         dev.schizo.client.gui.UiMotionPolicy.setReducedMotion(true);
         assertEquals(1.0F,animation.getValue());
         dev.schizo.client.gui.UiMotionPolicy.setReducedMotion(false);
         assertEquals(1.0F,animation.getValue());
      } finally {
         dev.schizo.client.gui.UiMotionPolicy.setReducedMotion(previous);
      }
   }

   @Test
   void identicalTargetsDoNotRestartActiveAnimation() {
      AtomicLong clock=new AtomicLong();
      FloatAnimation animation=new FloatAnimation(clock::get);
      animation.setTarget(1.0F,1000L);
      clock.set(400L*MILLIS);
      animation.setTarget(1.0F,1000L);
      clock.set(600L*MILLIS);
      assertEquals(0.6F,animation.getValue(),0.000001F);
   }
}
