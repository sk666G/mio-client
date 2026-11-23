/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils;

import dev.schizo.client.ClientAccess;
import net.minecraft.util.Mth;

public final class FrameTiming implements ClientAccess {
   private static final long NANOS_PER_SECOND = 1_000_000_000L;
   private static final int SAMPLE_CAPACITY = 4096;

   // A fixed ring avoids boxing and queue-node allocation once per rendered frame.
   private final long[] frameTimesNanos = new long[SAMPLE_CAPACITY];
   private int oldestSampleIndex;
   private int sampleCount;
   private long lastFrameTimeNanos;
   private long lastFrameRate;
   private long lastFrameDurationNanos;
   private int framesPerSecond;

   public int getFramesPerSecond() {
      return this.framesPerSecond;
   }

   /** Returns the duration of the most recently recorded frame in milliseconds. */
   public float getFrameTimeMillis() {
      return this.lastFrameDurationNanos / 1_000_000.0F;
   }

   public float getFrameFactor(float rateScale) {
      return Mth.clamp(1.0F / ((float)this.lastFrameRate * rateScale), 0.0F, 1.0F);
   }

   public void recordFrame() {
      long sampledTimeNanos = System.nanoTime();

      if (this.sampleCount == SAMPLE_CAPACITY) {
         this.oldestSampleIndex = (this.oldestSampleIndex + 1) % SAMPLE_CAPACITY;
         this.sampleCount--;
      }

      int writeIndex = (this.oldestSampleIndex + this.sampleCount) % SAMPLE_CAPACITY;
      this.frameTimesNanos[writeIndex] = sampledTimeNanos;
      this.sampleCount++;

      while (this.sampleCount > 0) {
         long oldestFrameTime = this.frameTimesNanos[this.oldestSampleIndex];
         if (sampledTimeNanos - oldestFrameTime <= NANOS_PER_SECOND) {
            break;
         }
         this.oldestSampleIndex = (this.oldestSampleIndex + 1) % SAMPLE_CAPACITY;
         this.sampleCount--;
      }

      this.framesPerSecond = this.sampleCount;
      long elapsedNanos = Math.max(1L, sampledTimeNanos - this.lastFrameTimeNanos);
      this.lastFrameDurationNanos = elapsedNanos;
      this.lastFrameRate = NANOS_PER_SECOND / elapsedNanos;
      this.lastFrameTimeNanos = sampledTimeNanos;
   }

   public FrameTiming() {
      this.lastFrameTimeNanos = System.nanoTime();
   }
}
