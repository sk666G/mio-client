/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils;

import dev.schizo.client.gui.UiMotionPolicy;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.Objects;
import net.minecraft.util.Mth;

public class FloatAnimation {
   private final LongSupplier nanoTime;
   private float startValue;
   private float targetValue;
   private long startedAtNanos;
   private long durationNanos;

   public FloatAnimation() {
      this(System::nanoTime);
   }

   FloatAnimation(LongSupplier nanoTime) {
      this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
   }

   public float getValue() {
      if (this.settleIfIdleOrReduced()) return this.targetValue;
      return this.valueAt(this.nanoTime.getAsLong());
   }

   public void setValue(float value) {
      this.startValue = value;
      this.targetValue = value;
      this.durationNanos = 0L;
   }

   public void setTarget(boolean targetState, long newDurationMillis) {
      float decodedTarget = targetState ? 1.0F : 0.0F;
      this.setTarget(decodedTarget, newDurationMillis);
   }

   public float getValue(Function easing) {
      if (this.settleIfIdleOrReduced()) return this.targetValue;
      long nowNanos = this.nanoTime.getAsLong();
      float progress = this.progressAt(nowNanos);
      if (progress >= 1.0F) {
         this.setValue(this.targetValue);
         return this.targetValue;
      }

      Float easingResult = (Float)easing.apply(progress);
      return easingResult * (this.targetValue - this.startValue) + this.startValue;
   }

   public void setTarget(float newTargetValue, long newDurationMillis) {
      if (UiMotionPolicy.isReducedMotion()) {
         this.setValue(newTargetValue);
         return;
      }
      if (this.targetValue != newTargetValue) {
         long nowNanos = this.nanoTime.getAsLong();
         this.startValue = this.valueAt(nowNanos);
         this.targetValue = newTargetValue;
         this.startedAtNanos = nowNanos;
         this.durationNanos = TimeUnit.MILLISECONDS.toNanos(Math.max(0L, newDurationMillis));
      }
   }

   private boolean settleIfIdleOrReduced() {
      if(this.durationNanos<=0L) return true;
      if(UiMotionPolicy.isReducedMotion()) {
         this.setValue(this.targetValue);
         return true;
      }
      return false;
   }

   private float valueAt(long nowNanos) {
      float progress = this.progressAt(nowNanos);
      if(progress>=1.0F) {
         this.setValue(this.targetValue);
         return this.targetValue;
      }
      return progress * (this.targetValue - this.startValue) + this.startValue;
   }

   private float progressAt(long nowNanos) {
      if (this.durationNanos <= 0L) {
         return 1.0F;
      }

      long elapsedNanos = nowNanos - this.startedAtNanos;
      if (elapsedNanos >= this.durationNanos) {
         return 1.0F;
      }

      float progress = (float)elapsedNanos / (float)this.durationNanos;
      return Mth.clamp(progress, 0.0F, 1.0F);
   }
}
