package dev.schizo.client.utils.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class ExplosionRaycastSnapshotBuilderTest {
   @Test
   void buildsRegionAroundTargetAndExplosionWithPadding() {
      ExplosionRaycastSnapshotBuilder.Region region =
         ExplosionRaycastSnapshotBuilder.Region.enclosingRays(
            new AABB(2.2D, 4.0D, -1.0D, 3.8D, 5.8D, 0.5D),
            new Vec3(8.1D, 1.2D, -4.0D)
         );

      assertEquals(1, region.minX());
      assertEquals(0, region.minY());
      assertEquals(-5, region.minZ());
      assertEquals(9, region.maxX());
      assertEquals(6, region.maxY());
      assertEquals(1, region.maxZ());
      assertEquals(9L * 7L * 7L, region.volume());
   }

   @Test
   void enclosesManyCandidateExplosionEndpointsInOneCaptureRegion() {
      ExplosionRaycastSnapshotBuilder.Region region =
         ExplosionRaycastSnapshotBuilder.Region.enclosingRays(
            new AABB(2.2D, 4.0D, -1.0D, 3.8D, 5.8D, 0.5D),
            new AABB(8.1D, 1.2D, -4.0D, 12.0D, 2.0D, 7.0D),
            1
         );

      assertEquals(1, region.minX());
      assertEquals(0, region.minY());
      assertEquals(-5, region.minZ());
      assertEquals(13, region.maxX());
      assertEquals(6, region.maxY());
      assertEquals(8, region.maxZ());
   }

   @Test
   void supportsCustomPaddingAndZeroPadding() {
      ExplosionRaycastSnapshotBuilder.Region region =
         ExplosionRaycastSnapshotBuilder.Region.enclosingRays(
            new AABB(0.1D, 0.1D, 0.1D, 0.9D, 0.9D, 0.9D),
            new Vec3(1.1D, 1.1D, 1.1D),
            0
         );

      assertEquals(0, region.minX());
      assertEquals(0, region.minY());
      assertEquals(0, region.minZ());
      assertEquals(1, region.maxX());
      assertEquals(1, region.maxY());
      assertEquals(1, region.maxZ());
   }

   @Test
   void rejectsNegativePaddingAndInvertedRegions() {
      assertThrows(IllegalArgumentException.class, () ->
         ExplosionRaycastSnapshotBuilder.Region.enclosingRays(
            new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D),
            Vec3.ZERO,
            -1
         ));
      assertThrows(IllegalArgumentException.class, () ->
         new ExplosionRaycastSnapshotBuilder.Region(1, 0, 0, 0, 0, 0));
   }

   @Test
   void rejectsCoordinatesOutsideBlockPositionRange() {
      assertThrows(IllegalArgumentException.class, () ->
         ExplosionRaycastSnapshotBuilder.Region.enclosingRays(
            new AABB(Double.MAX_VALUE, 0.0D, 0.0D, Double.MAX_VALUE, 1.0D, 1.0D),
            Vec3.ZERO
         ));
   }

   @Test
   void acceptsCaptureLimitsAtOrBelowTheDefaults() {
      ExplosionRaycastSnapshotBuilder.validateCaptureLimits(1, 1L);
      ExplosionRaycastSnapshotBuilder.validateCaptureLimits(
         ExplosionRaycastSnapshotBuilder.DEFAULT_MAX_CAPTURED_BLOCKS,
         ExplosionRaycastSnapshotBuilder.DEFAULT_MAX_CAPTURE_NANOS
      );
      ExplosionRaycastSnapshotBuilder.validateCaptureLimits(
         256,
         2_000_000L
      );
   }

   @Test
   void rejectsCaptureLimitsThatWouldDisableOrRaiseTheHardCaps() {
      assertThrows(IllegalArgumentException.class, () ->
         ExplosionRaycastSnapshotBuilder.validateCaptureLimits(0, 1L));
      assertThrows(IllegalArgumentException.class, () ->
         ExplosionRaycastSnapshotBuilder.validateCaptureLimits(
            ExplosionRaycastSnapshotBuilder.DEFAULT_MAX_CAPTURED_BLOCKS + 1, 1L));
      assertThrows(IllegalArgumentException.class, () ->
         ExplosionRaycastSnapshotBuilder.validateCaptureLimits(1, 0L));
      assertThrows(IllegalArgumentException.class, () ->
         ExplosionRaycastSnapshotBuilder.validateCaptureLimits(
            1, ExplosionRaycastSnapshotBuilder.DEFAULT_MAX_CAPTURE_NANOS + 1L));
   }

   @Test
   void captureBudgetExceptionReportsPartialWorkWithoutPublishingASnapshot() {
      ExplosionRaycastSnapshotBuilder.CaptureBudgetExceededException failure =
         new ExplosionRaycastSnapshotBuilder.CaptureBudgetExceededException(64, 256L, 8_000_000L);

      assertEquals(64, failure.capturedBlocks());
      assertEquals(256L, failure.requestedBlocks());
      assertEquals(8_000_000L, failure.budgetNanos());
      assertEquals(true, failure.getMessage().contains("64 of 256 blocks"));
   }

   @Test
   void exposesInclusiveMinimumAndMaximumPositions() {
      ExplosionRaycastSnapshotBuilder.Region region =
         new ExplosionRaycastSnapshotBuilder.Region(-2, 3, -4, 5, 6, 7);

      assertEquals(-2, region.minimum().getX());
      assertEquals(3, region.minimum().getY());
      assertEquals(-4, region.minimum().getZ());
      assertEquals(5, region.maximum().getX());
      assertEquals(6, region.maximum().getY());
      assertEquals(7, region.maximum().getZ());
      assertEquals(8L * 4L * 12L, region.volume());
   }
}
