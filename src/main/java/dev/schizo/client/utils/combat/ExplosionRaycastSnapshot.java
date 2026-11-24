/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils.combat;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Immutable block-geometry snapshot for worker-side explosion exposure and
 * hit/miss-only ray queries. It does not return a hit face or hit position.
 * The snapshot contains no BlockState, Level, Entity,
 * CollisionContext, or mutable world manager references.
 *
 * <p>All coordinates in BlockGeometry.collisionBoxes are block-local, just
 * like VoxelShape.toAabbs(). Callers must capture every position a ray may
 * traverse. A missing position returns INCOMPLETE rather than being treated
 * as air.</p>
 */
public final class ExplosionRaycastSnapshot {
   private static final List<AABB> FULL_BLOCK = List.of(new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D));

   public enum Result {
      HIT,
      MISS,
      INCOMPLETE
   }

   public record BlockGeometry(float explosionResistance, List<AABB> collisionBoxes) {
      public BlockGeometry {
         collisionBoxes = List.copyOf(Objects.requireNonNull(collisionBoxes, "collisionBoxes"));
      }
   }

   /**
    * Mirrors the current classifier priority: ignored positions first,
    * explicit solid position second, and low-resistance filtering last.
    */
   public record Query(Set<Long> ignoredBlockPositions, Long solidBlockPosition, boolean ignoreLowResistance) {
      public Query {
         ignoredBlockPositions = Set.copyOf(
            Objects.requireNonNull(ignoredBlockPositions, "ignoredBlockPositions")
         );
      }

      public static Query normal() {
         return new Query(Set.of(), null, false);
      }
   }

   private record RayRequest(
      ExplosionRaycastSnapshot snapshot,
      Query query,
      Vec3 start,
      Vec3 end
   ) {
   }

   private final long worldGeneration;
   private final long captureTick;
   private final String dimensionId;
   private final Map<Long, BlockGeometry> blocks;

   public ExplosionRaycastSnapshot(
      long worldGeneration,
      long captureTick,
      String dimensionId,
      Map<Long, BlockGeometry> blocks
   ) {
      this.worldGeneration = worldGeneration;
      this.captureTick = captureTick;
      this.dimensionId = Objects.requireNonNull(dimensionId, "dimensionId");
      this.blocks = Map.copyOf(Objects.requireNonNull(blocks, "blocks"));
   }

   public long worldGeneration() {
      return this.worldGeneration;
   }

   public long captureTick() {
      return this.captureTick;
   }

   public String dimensionId() {
      return this.dimensionId;
   }

   public int capturedBlockCount() {
      return this.blocks.size();
   }

   /**
    * Traverses only immutable snapshot data. BlockGetter.traverseBlocks is a
    * static coordinate DDA routine; it is used here without a live BlockGetter.
    */
   public Result raycast(Vec3 start, Vec3 end, Query query) {
      Objects.requireNonNull(start, "start");
      Objects.requireNonNull(end, "end");
      Objects.requireNonNull(query, "query");

      RayRequest request = new RayRequest(this, query, start, end);
      return BlockGetter.traverseBlocks(
         start,
         end,
         request,
         ExplosionRaycastSnapshot::visitBlock,
         ignored -> Result.MISS
      );
   }

   private static Result visitBlock(RayRequest request, BlockPos position) {
      long packedPosition = position.asLong();
      BlockGeometry geometry = request.snapshot().blocks.get(packedPosition);
      if (geometry == null) {
         return Result.INCOMPLETE;
      }

      Query query = request.query();
      if (query.ignoredBlockPositions().contains(packedPosition)) {
         return null;
      }

      if (query.solidBlockPosition() != null
         && query.solidBlockPosition().longValue() == packedPosition) {
         return intersects(FULL_BLOCK, request.start(), request.end(), position)
            ? Result.HIT
            : null;
      }

      if (query.ignoreLowResistance() && geometry.explosionResistance() < 600.0F) {
         return null;
      }

      return intersects(geometry.collisionBoxes(), request.start(), request.end(), position)
         ? Result.HIT
         : null;
   }

   private static boolean intersects(
      List<AABB> localBoxes,
      Vec3 start,
      Vec3 end,
      BlockPos blockPosition
   ) {
      if (localBoxes.isEmpty()) {
         return false;
      }

      Vec3 difference = end.subtract(start);
      if (difference.lengthSqr() < 1.0E-7D) {
         return false;
      }

      // VoxelShape.clip first checks whether the point 0.001 along the ray is
      // inside a full voxel. This matters when both ray endpoints are inside
      // a shape, where a boundary-only AABB clip would otherwise miss.
      Vec3 sample = start.add(difference.scale(0.001D));
      double localX = sample.x - blockPosition.getX();
      double localY = sample.y - blockPosition.getY();
      double localZ = sample.z - blockPosition.getZ();
      for (AABB box : localBoxes) {
         if (localX >= box.minX && localX < box.maxX
            && localY >= box.minY && localY < box.maxY
            && localZ >= box.minZ && localZ < box.maxZ) {
            return true;
         }
      }

      // The boxes are the copied output of VoxelShape.toAabbs(), so this
      // static helper needs only immutable geometry and the current ray.
      BlockHitResult hit = AABB.clip(localBoxes, start, end, blockPosition);
      return hit != null;
   }
}
