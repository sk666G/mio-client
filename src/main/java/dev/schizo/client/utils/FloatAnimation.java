/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils;

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
      return this.valueAt(this.nanoTime.getAsLong());
   }

   public void setValue(float value) {
      this.setTarget(value, 0L);
   }

   public void setTarget(boolean targetState, long newDurationMillis) {
      float decodedTarget = targetState ? 1.0F : 0.0F;
      this.setTarget(decodedTarget, newDurationMillis);
   }

   public float getValue(Function easing) {
      long nowNanos = this.nanoTime.getAsLong();
      float progress = this.progressAt(nowNanos);
      if (progress >= 1.0F) {
         return this.targetValue;
      }

      Float easingResult = (Float)easing.apply(progress);
      return easingResult * (this.targetValue - this.startValue) + this.startValue;
   }

   public void setTarget(float newTargetValue, long newDurationMillis) {
      if (this.targetValue != newTargetValue) {
         long nowNanos = this.nanoTime.getAsLong();
         this.startValue = this.valueAt(nowNanos);
         this.targetValue = newTargetValue;
         this.startedAtNanos = nowNanos;
         this.durationNanos = TimeUnit.MILLISECONDS.toNanos(Math.max(0L, newDurationMillis));
      }
   }

   private float valueAt(long nowNanos) {
      float progress = this.progressAt(nowNanos);
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
