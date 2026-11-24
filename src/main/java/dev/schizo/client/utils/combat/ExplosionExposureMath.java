/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils.combat;

import java.util.Objects;

/**
 * Pure explosion-exposure sample generation.
 *
 * <p>The caller supplies a ray visibility tester. For worker use, that tester
 * must operate only on an immutable ray/block snapshot.</p>
 */
public final class ExplosionExposureMath {
   private ExplosionExposureMath() {
   }

   @FunctionalInterface
   public interface VisibilityTester {
      boolean isVisible(
         double sampleX,
         double sampleY,
         double sampleZ,
         double explosionX,
         double explosionY,
         double explosionZ
      );
   }

   public static float calculate(
      double minX,
      double minY,
      double minZ,
      double maxX,
      double maxY,
      double maxZ,
      double explosionX,
      double explosionY,
      double explosionZ,
      VisibilityTester visibilityTester
   ) {
      Objects.requireNonNull(visibilityTester, "visibilityTester");

      double stepX = 1.0D / ((maxX - minX) * 2.0D + 1.0D);
      double stepY = 1.0D / ((maxY - minY) * 2.0D + 1.0D);
      double stepZ = 1.0D / ((maxZ - minZ) * 2.0D + 1.0D);
      double offsetX = (1.0D - Math.floor(1.0D / stepX) * stepX) / 2.0D;
      double offsetZ = (1.0D - Math.floor(1.0D / stepZ) * stepZ) / 2.0D;
      if (stepX < 0.0D || stepY < 0.0D || stepZ < 0.0D) {
         return 0.0F;
      }

      int visibleSamples = 0;
      int totalSamples = 0;
      for (double fractionX = 0.0D; fractionX <= 1.0D; fractionX += stepX) {
         for (double fractionY = 0.0D; fractionY <= 1.0D; fractionY += stepY) {
            for (double fractionZ = 0.0D; fractionZ <= 1.0D; fractionZ += stepZ) {
               double sampleX = lerp(fractionX, minX, maxX);
               double sampleY = lerp(fractionY, minY, maxY);
               double sampleZ = lerp(fractionZ, minZ, maxZ);
               double offsetSampleX = sampleX + offsetX;
               double shiftedZ = sampleZ + offsetZ;

               if (visibilityTester.isVisible(
                  offsetSampleX,
                  sampleY,
                  shiftedZ,
                  explosionX,
                  explosionY,
                  explosionZ
               )) {
                  visibleSamples++;
               }
               totalSamples++;
            }
         }
      }

      return (float)visibleSamples / totalSamples;
   }

   private static double lerp(double fraction, double start, double end) {
      return start + fraction * (end - start);
   }
}
