package dev.schizo.client.utils.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ExplosionExposureMathTest {
   @Test
   void samplesAllEightCornersForDegenerateBounds() {
      AtomicInteger calls = new AtomicInteger();

      float exposure = ExplosionExposureMath.calculate(
         2.0D, 3.0D, 4.0D,
         2.0D, 3.0D, 4.0D,
         10.0D, 11.0D, 12.0D,
         (sampleX, sampleY, sampleZ, explosionX, explosionY, explosionZ) -> calls.getAndIncrement() < 4
      );

      assertEquals(8, calls.get());
      assertEquals(0.5F, exposure);
   }

   @Test
   void forwardsTheExplosionEndpointToEachVisibilityTest() {
      AtomicInteger calls = new AtomicInteger();

      ExplosionExposureMath.calculate(
         0.0D, 0.0D, 0.0D,
         0.0D, 0.0D, 0.0D,
         7.0D, 8.0D, 9.0D,
         (sampleX, sampleY, sampleZ, explosionX, explosionY, explosionZ) -> {
            assertEquals(7.0D, explosionX);
            assertEquals(8.0D, explosionY);
            assertEquals(9.0D, explosionZ);
            calls.incrementAndGet();
            return true;
         }
      );

      assertEquals(8, calls.get());
   }

   @Test
   void doesNotCallVisibilityTesterWhenSampleStepIsNegative() {
      AtomicInteger calls = new AtomicInteger();

      float exposure = ExplosionExposureMath.calculate(
         0.0D, 0.0D, 0.0D,
         -1.0D, 1.0D, 1.0D,
         0.0D, 0.0D, 0.0D,
         (sampleX, sampleY, sampleZ, explosionX, explosionY, explosionZ) -> {
            calls.incrementAndGet();
            return true;
         }
      );

      assertEquals(0.0F, exposure);
      assertEquals(0, calls.get());
   }

   @Test
   void detailedCalculationStopsAndMarksUnknownRayAsIncomplete() {
      AtomicInteger calls = new AtomicInteger();

      ExplosionExposureMath.ExposureResult result = ExplosionExposureMath.calculateDetailed(
         0.0D, 0.0D, 0.0D,
         0.0D, 0.0D, 0.0D,
         1.0D, 1.0D, 1.0D,
         (sampleX, sampleY, sampleZ, explosionX, explosionY, explosionZ) -> {
            calls.incrementAndGet();
            return ExplosionExposureMath.Visibility.INCOMPLETE;
         }
      );

      assertEquals(false, result.complete());
      assertEquals(0.0F, result.exposure());
      assertEquals(1, result.totalSamples());
      assertEquals(1, calls.get());
   }

   @Test
   void snapshotExposureUsesCapturedBlockGeometryAndReportsCompletion() {
      ExplosionRaycastSnapshot emptySnapshot = new ExplosionRaycastSnapshot(
         7L,
         99L,
         "minecraft:overworld",
         java.util.Map.of(
            net.minecraft.core.BlockPos.ZERO.asLong(),
            new ExplosionRaycastSnapshot.BlockGeometry(0.0F, java.util.List.of())
         )
      );

      ExplosionExposureMath.ExposureResult visible = ExplosionExposureMath.calculateFromSnapshot(
         0.0D, 0.0D, 0.0D,
         0.0D, 0.0D, 0.0D,
         0.5D, 0.5D, 0.5D,
         emptySnapshot,
         ExplosionRaycastSnapshot.Query.normal()
      );

      assertEquals(true, visible.complete());
      assertEquals(1.0F, visible.exposure());
      assertEquals(8, visible.totalSamples());

      ExplosionRaycastSnapshot incompleteSnapshot = new ExplosionRaycastSnapshot(
         7L,
         99L,
         "minecraft:overworld",
         java.util.Map.of()
      );
      ExplosionExposureMath.ExposureResult incomplete = ExplosionExposureMath.calculateFromSnapshot(
         0.0D, 0.0D, 0.0D,
         0.0D, 0.0D, 0.0D,
         0.5D, 0.5D, 0.5D,
         incompleteSnapshot,
         ExplosionRaycastSnapshot.Query.normal()
      );

      assertEquals(false, incomplete.complete());
      assertEquals(0.0F, incomplete.exposure());
      assertEquals(1, incomplete.totalSamples());
   }

   @Test
   void requiresAVisibilityTester() {
      assertThrows(NullPointerException.class, () -> ExplosionExposureMath.calculate(
         0.0D, 0.0D, 0.0D,
         1.0D, 1.0D, 1.0D,
         0.0D, 0.0D, 0.0D,
         null
      ));
   }
}
