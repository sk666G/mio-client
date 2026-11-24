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
import net.minecraft.world.phys.Vec3;

/**
 * Pure explosion-exposure sample generation.
 *
 * <p>The caller supplies a ray visibility tester. For worker use, that tester
 * must operate only on an immutable ray/block snapshot.</p>
 */
public final class ExplosionExposureMath {
   private ExplosionExposureMath() {
   }

   public enum Visibility {
      VISIBLE,
      OCCLUDED,
      INCOMPLETE
   }

   public record ExposureResult(
      float exposure,
      int visibleSamples,
      int totalSamples,
      boolean complete
   ) {
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

   @FunctionalInterface
   public interface DetailedVisibilityTester {
      Visibility test(
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
      return calculateDetailed(
         minX, minY, minZ,
         maxX, maxY, maxZ,
         explosionX, explosionY, explosionZ,
         (sampleX, sampleY, sampleZ, endX, endY, endZ) ->
            visibilityTester.isVisible(sampleX, sampleY, sampleZ, endX, endY, endZ)
               ? Visibility.VISIBLE
               : Visibility.OCCLUDED
      ).exposure();
   }

   /**
    * Snapshot-only exposure calculation. An uncaptured ray cell marks the
    * result incomplete; callers must discard it rather than treating unknown
    * geometry as air or as an occluder.
    */
   public static ExposureResult calculateFromSnapshot(
      double minX,
      double minY,
      double minZ,
      double maxX,
      double maxY,
      double maxZ,
      double explosionX,
      double explosionY,
      double explosionZ,
      ExplosionRaycastSnapshot snapshot,
      ExplosionRaycastSnapshot.Query query
   ) {
      Objects.requireNonNull(snapshot, "snapshot");
      Objects.requireNonNull(query, "query");
      return calculateDetailed(
         minX, minY, minZ,
         maxX, maxY, maxZ,
         explosionX, explosionY, explosionZ,
         (sampleX, sampleY, sampleZ, endX, endY, endZ) -> {
            ExplosionRaycastSnapshot.Result result = snapshot.raycast(
               new Vec3(sampleX, sampleY, sampleZ),
               new Vec3(endX, endY, endZ),
               query
            );
            return switch (result) {
               case HIT -> Visibility.OCCLUDED;
               case MISS -> Visibility.VISIBLE;
               case INCOMPLETE -> Visibility.INCOMPLETE;
            };
         }
      );
   }

   public static ExposureResult calculateDetailed(
      double minX,
      double minY,
      double minZ,
      double maxX,
      double maxY,
      double maxZ,
      double explosionX,
      double explosionY,
      double explosionZ,
      DetailedVisibilityTester visibilityTester
   ) {
      Objects.requireNonNull(visibilityTester, "visibilityTester");

      double stepX = 1.0D / ((maxX - minX) * 2.0D + 1.0D);
      double stepY = 1.0D / ((maxY - minY) * 2.0D + 1.0D);
      double stepZ = 1.0D / ((maxZ - minZ) * 2.0D + 1.0D);
      double offsetX = (1.0D - Math.floor(1.0D / stepX) * stepX) / 2.0D;
      double offsetZ = (1.0D - Math.floor(1.0D / stepZ) * stepZ) / 2.0D;
      if (stepX < 0.0D || stepY < 0.0D || stepZ < 0.0D) {
         return new ExposureResult(0.0F, 0, 0, true);
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

               Visibility visibility = Objects.requireNonNull(
                  visibilityTester.test(
                     offsetSampleX,
                     sampleY,
                     shiftedZ,
                     explosionX,
                     explosionY,
                     explosionZ
                  ),
                  "visibility result"
               );
               totalSamples++;
               if (visibility == Visibility.INCOMPLETE) {
                  return new ExposureResult(0.0F, visibleSamples, totalSamples, false);
               }
               if (visibility == Visibility.VISIBLE) {
                  visibleSamples++;
               }
            }
         }
      }

      return new ExposureResult(
         (float)visibleSamples / totalSamples,
         visibleSamples,
         totalSamples,
         true
      );
   }

   private static double lerp(double fraction, double start, double end) {
      return start + fraction * (end - start);
   }
}
