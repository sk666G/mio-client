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
   private static final int WINDOW_BUCKETS = 1001;

   private final long[] bucketIds = new long[WINDOW_BUCKETS];
   private final int[] bucketCounts = new int[WINDOW_BUCKETS];
   private long latestBucket = Long.MIN_VALUE;
   private int windowFrameCount;
   private long lastFrameTimeNanos;
   private long lastFrameRate;
   private int framesPerSecond;
   private boolean hasPreviousFrame;

   public int getFramesPerSecond() {
      return this.framesPerSecond;
   }

   public float getFrameFactor(float rateScale) {
      return Mth.clamp(1.0F / ((float)this.lastFrameRate * rateScale), 0.0F, 1.0F);
   }

   public void recordFrame() {
      this.recordFrameAt(System.nanoTime());
   }

   void recordFrameAt(long sampledTimeNanos) {
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

      if (this.hasPreviousFrame) {
         long elapsedNanos = Math.max(1L, sampledTimeNanos - this.lastFrameTimeNanos);
         this.lastFrameRate = NANOS_PER_SECOND / elapsedNanos;
      }
      this.lastFrameTimeNanos = sampledTimeNanos;
      this.hasPreviousFrame = true;
   }

   public FrameTiming() {
      Arrays.fill(this.bucketIds, Long.MIN_VALUE);
      this.lastFrameTimeNanos = System.nanoTime();
   }
}
