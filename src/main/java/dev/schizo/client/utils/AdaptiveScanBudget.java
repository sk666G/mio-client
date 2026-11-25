/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils;

/** Conservative frame-pressure scaling for incremental background/client scans. */
public final class AdaptiveScanBudget {
   private AdaptiveScanBudget() {
   }

   public static int limit(int baseLimit, FrameTiming timing) {
      if (timing == null) {
         return baseLimit;
      }
      return limit(
         baseLimit,
         timing.getFramesPerSecond(),
         timing.getAverageFrameTimeMillis(),
         timing.getStuttersPerSecond()
      );
   }

   public static long nanos(long baseNanos, FrameTiming timing) {
      if (timing == null) {
         return baseNanos;
      }
      float factor = factor(
         timing.getFramesPerSecond(),
         timing.getAverageFrameTimeMillis(),
         timing.getStuttersPerSecond()
      );
      return Math.max(100_000L, Math.round(baseNanos * factor));
   }

   static int limit(int baseLimit, int fps, float averageFrameMillis, int stuttersPerSecond) {
      if (baseLimit < 1) {
         throw new IllegalArgumentException("Base scan limit must be positive");
      }
      return Math.max(1, Math.round(baseLimit * factor(fps, averageFrameMillis, stuttersPerSecond)));
   }

   static float factor(int fps, float averageFrameMillis, int stuttersPerSecond) {
      float factor;
      if (fps > 0 && fps <= 20 || averageFrameMillis >= 50.0F) {
         factor = 0.25F;
      } else if (fps > 0 && fps <= 35 || averageFrameMillis >= 28.0F) {
         factor = 0.5F;
      } else if (fps > 0 && fps <= 55 || averageFrameMillis >= 18.0F) {
         factor = 0.75F;
      } else {
         factor = 1.0F;
      }
      if (stuttersPerSecond >= 4) {
         factor = Math.min(factor, 0.5F);
      }
      return factor;
   }
}
