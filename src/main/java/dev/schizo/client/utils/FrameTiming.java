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

import java.util.Arrays;
import net.minecraft.util.Mth;

public final class FrameTiming implements ClientAccess {
   private static final long NANOS_PER_SECOND = 1_000_000_000L;
   private static final int SAMPLE_CAPACITY = 16384;
   private static final long ONE_PERCENT_LOW_REFRESH_NANOS = 250_000_000L;

   // A fixed ring avoids boxing and queue-node allocation once per rendered frame.
   private final long[] frameTimesNanos = new long[SAMPLE_CAPACITY];
   private final long[] frameDurationsScratch = new long[SAMPLE_CAPACITY - 1];
   private int oldestSampleIndex;
   private int sampleCount;
   private long lastFrameTimeNanos;
   private long lastFrameRate;
   private long lastFrameDurationNanos;
   private int framesPerSecond;
   private long lastOnePercentLowUpdateNanos;
   private float onePercentLowFramesPerSecond;

   public int getFramesPerSecond() {
      return this.framesPerSecond;
   }

   /** Returns the duration of the most recently recorded frame in milliseconds. */
   public float getFrameTimeMillis() {
      return this.lastFrameDurationNanos / 1_000_000.0F;
   }

   /** Returns the average interval between frames in the current one-second sample window. */
   public float getAverageFrameTimeMillis() {
      if (this.sampleCount < 2) {
         return this.getFrameTimeMillis();
      }

      long firstFrameTime = this.frameTimesNanos[this.oldestSampleIndex];
      long elapsedNanos = this.frameTimesNanos[(this.oldestSampleIndex + this.sampleCount - 1) % SAMPLE_CAPACITY] - firstFrameTime;
      return elapsedNanos / (float)(this.sampleCount - 1) / 1_000_000.0F;
   }

   /** Returns FPS derived from the 99th-percentile frame time in the rolling sample window. */
   public float getOnePercentLowFramesPerSecond() {
      long currentTimeNanos = System.nanoTime();
      if (currentTimeNanos - this.lastOnePercentLowUpdateNanos >= ONE_PERCENT_LOW_REFRESH_NANOS) {
         int durationCount = this.sampleCount - 1;
         if (durationCount > 0) {
            for (int i = 0; i < durationCount; i++) {
               int previousIndex = (this.oldestSampleIndex + i) % SAMPLE_CAPACITY;
               int currentIndex = (previousIndex + 1) % SAMPLE_CAPACITY;
               this.frameDurationsScratch[i] = Math.max(1L, this.frameTimesNanos[currentIndex] - this.frameTimesNanos[previousIndex]);
            }

            Arrays.sort(this.frameDurationsScratch, 0, durationCount);
            int percentileIndex = Math.min(durationCount - 1, (int)Math.ceil(durationCount * 0.99D) - 1);
            this.onePercentLowFramesPerSecond = (float)(NANOS_PER_SECOND / (double)this.frameDurationsScratch[percentileIndex]);
         } else {
            this.onePercentLowFramesPerSecond = 0.0F;
         }

         this.lastOnePercentLowUpdateNanos = currentTimeNanos;
      }

      return this.onePercentLowFramesPerSecond;
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
