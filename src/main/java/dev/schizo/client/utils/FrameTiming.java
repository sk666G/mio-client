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
   private static final long NANOS_PER_BUCKET = 1_000_000L;
   private static final long LOW_FPS_REFRESH_NANOS = 250_000_000L;
   private static final long ONE_SECOND_NANOS = NANOS_PER_SECOND;
   private static final int WINDOW_BUCKETS = 1001;
   private static final int FRAME_SAMPLE_CAPACITY = 65536;

   private final long[] bucketIds = new long[WINDOW_BUCKETS];
   private final int[] bucketCounts = new int[WINDOW_BUCKETS];

   // A fixed-size ring keeps frame history bounded and allocation-free during rendering.
   private final long[] frameSampleTimesNanos = new long[FRAME_SAMPLE_CAPACITY];
   private final long[] frameDurationsNanos = new long[FRAME_SAMPLE_CAPACITY];
   private final long[] percentileScratch = new long[FRAME_SAMPLE_CAPACITY];

   private long latestBucket = Long.MIN_VALUE;
   private int windowFrameCount;
   private long lastFrameTimeNanos;
   private long lastFrameRate;
   private long lastFrameDurationNanos;
   private long frameSequence;
   private boolean hasPreviousFrame;

   private int oldestFrameSampleIndex;
   private int frameSampleCount;
   private long totalFrameDurationNanos;

   private long lastLowFpsUpdateNanos = Long.MIN_VALUE;
   private float cachedOnePercentLowFramesPerSecond;

   public int getFramesPerSecond() {
      return this.framesPerSecond;
   }

   private int framesPerSecond;

   /** Returns the number of rendered frames recorded since this tracker was created. */
   public long getFrameSequence() {
      return this.frameSequence;
   }

   /** Returns the most recent frame duration in milliseconds, or zero before two frames. */
   public float getFrameTimeMillis() {
      return this.lastFrameDurationNanos / 1_000_000.0F;
   }

   /** Returns the average frame duration in the rolling one-second history window. */
   public float getAverageFrameTimeMillis() {
      if (this.frameSampleCount == 0) {
         return 0.0F;
      }

      return this.totalFrameDurationNanos / (float)this.frameSampleCount / 1_000_000.0F;
   }

   /** Returns FPS derived from the 99th-percentile frame duration in the rolling window. */
   public float getOnePercentLowFramesPerSecond() {
      return this.getOnePercentLowFramesPerSecondAt(System.nanoTime());
   }

   float getOnePercentLowFramesPerSecondAt(long currentTimeNanos) {
      if (this.lastLowFpsUpdateNanos == Long.MIN_VALUE
         || currentTimeNanos - this.lastLowFpsUpdateNanos >= LOW_FPS_REFRESH_NANOS) {
         if (this.frameSampleCount == 0) {
            this.cachedOnePercentLowFramesPerSecond = 0.0F;
         } else {
            for (int i = 0; i < this.frameSampleCount; i++) {
               int index = (this.oldestFrameSampleIndex + i) % FRAME_SAMPLE_CAPACITY;
               this.percentileScratch[i] = this.frameDurationsNanos[index];
            }

            Arrays.sort(this.percentileScratch, 0, this.frameSampleCount);
            int percentileIndex = Math.min(
               this.frameSampleCount - 1,
               (int)Math.ceil(this.frameSampleCount * 0.99D) - 1
            );
            long percentileFrameTime = Math.max(1L, this.percentileScratch[percentileIndex]);
            this.cachedOnePercentLowFramesPerSecond = (float)(NANOS_PER_SECOND / (double)percentileFrameTime);
         }

         this.lastLowFpsUpdateNanos = currentTimeNanos;
      }

      return this.cachedOnePercentLowFramesPerSecond;
   }

   public float getFrameFactor(float rateScale) {
      if (rateScale <= 0.0F) {
         return 0.0F;
      }
      return Mth.clamp(1.0F / ((float)this.lastFrameRate * rateScale), 0.0F, 1.0F);
   }

   public void recordFrame() {
      this.recordFrameAt(System.nanoTime());
   }

   void recordFrameAt(long sampledTimeNanos) {
      boolean clockMovedBackwards = this.hasPreviousFrame && sampledTimeNanos < this.lastFrameTimeNanos;
      this.recordFpsBucket(sampledTimeNanos);

      if (clockMovedBackwards) {
         this.clearFrameSamples();
         this.lastFrameDurationNanos = 0L;
         this.lastFrameRate = 0L;
      } else if (this.hasPreviousFrame) {
         long elapsedNanos = Math.max(1L, sampledTimeNanos - this.lastFrameTimeNanos);
         this.lastFrameDurationNanos = elapsedNanos;
         this.lastFrameRate = NANOS_PER_SECOND / elapsedNanos;
         this.appendFrameSample(sampledTimeNanos, elapsedNanos);
      } else {
         this.lastFrameDurationNanos = 0L;
      }

      this.lastFrameTimeNanos = sampledTimeNanos;
      this.hasPreviousFrame = true;
      this.frameSequence++;
   }

   private void recordFpsBucket(long sampledTimeNanos) {
      long bucket = Math.floorDiv(sampledTimeNanos, NANOS_PER_BUCKET);
      if (this.latestBucket == Long.MIN_VALUE || bucket < this.latestBucket
         || bucket - this.latestBucket >= WINDOW_BUCKETS) {
         Arrays.fill(this.bucketIds, Long.MIN_VALUE);
         Arrays.fill(this.bucketCounts, 0);
         this.windowFrameCount = 0;
      } else if (bucket > this.latestBucket) {
         long oldFirstBucket = this.latestBucket - WINDOW_BUCKETS + 1L;
         long newFirstBucket = bucket - WINDOW_BUCKETS + 1L;
         for (long expired = oldFirstBucket; expired < newFirstBucket; expired++) {
            int index = (int)Math.floorMod(expired, WINDOW_BUCKETS);
            if (this.bucketIds[index] == expired) {
               this.windowFrameCount -= this.bucketCounts[index];
               this.bucketCounts[index] = 0;
               this.bucketIds[index] = Long.MIN_VALUE;
            }
         }
      }

      int index = (int)Math.floorMod(bucket, WINDOW_BUCKETS);
      if (this.bucketIds[index] != bucket) {
         if (this.bucketIds[index] != Long.MIN_VALUE) {
            this.windowFrameCount -= this.bucketCounts[index];
         }
         this.bucketIds[index] = bucket;
         this.bucketCounts[index] = 0;
      }
      this.bucketCounts[index]++;
      this.windowFrameCount++;
      this.latestBucket = bucket;
      this.framesPerSecond = this.windowFrameCount;
   }

   private void appendFrameSample(long sampledTimeNanos, long frameDurationNanos) {
      if (this.frameSampleCount == FRAME_SAMPLE_CAPACITY) {
         this.removeOldestFrameSample();
      }

      int writeIndex = (this.oldestFrameSampleIndex + this.frameSampleCount) % FRAME_SAMPLE_CAPACITY;
      this.frameSampleTimesNanos[writeIndex] = sampledTimeNanos;
      this.frameDurationsNanos[writeIndex] = frameDurationNanos;
      this.frameSampleCount++;
      this.totalFrameDurationNanos += frameDurationNanos;

      while (this.frameSampleCount > 0
         && sampledTimeNanos - this.frameSampleTimesNanos[this.oldestFrameSampleIndex] > ONE_SECOND_NANOS) {
         this.removeOldestFrameSample();
      }
   }

   private void removeOldestFrameSample() {
      this.totalFrameDurationNanos -= this.frameDurationsNanos[this.oldestFrameSampleIndex];
      this.oldestFrameSampleIndex = (this.oldestFrameSampleIndex + 1) % FRAME_SAMPLE_CAPACITY;
      this.frameSampleCount--;
   }

   private void clearFrameSamples() {
      this.oldestFrameSampleIndex = 0;
      this.frameSampleCount = 0;
      this.totalFrameDurationNanos = 0L;
      this.lastLowFpsUpdateNanos = Long.MIN_VALUE;
      this.cachedOnePercentLowFramesPerSecond = 0.0F;
   }

   public FrameTiming() {
      Arrays.fill(this.bucketIds, Long.MIN_VALUE);
      this.lastFrameTimeNanos = System.nanoTime();
   }
}
