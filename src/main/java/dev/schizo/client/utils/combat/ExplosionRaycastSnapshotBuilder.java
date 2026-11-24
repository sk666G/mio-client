/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Client-thread-only capture adapter for ExplosionRaycastSnapshot.
 *
 * <p>The returned snapshot contains only block-local AABB values and numeric
 * block resistance. No live Minecraft object is retained by the snapshot.</p>
 */
public final class ExplosionRaycastSnapshotBuilder {
   public static final int DEFAULT_MAX_CAPTURED_BLOCKS = 32_768;
   private static final int DEFAULT_REGION_PADDING = 1;

   private ExplosionRaycastSnapshotBuilder() {
   }

   public record Region(
      int minX,
      int minY,
      int minZ,
      int maxX,
      int maxY,
      int maxZ
   ) {
      public Region {
         if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("Snapshot region minimum exceeds maximum");
         }
      }

      public long volume() {
         long sizeX = (long)this.maxX - this.minX + 1L;
         long sizeY = (long)this.maxY - this.minY + 1L;
         long sizeZ = (long)this.maxZ - this.minZ + 1L;
         return Math.multiplyExact(Math.multiplyExact(sizeX, sizeY), sizeZ);
      }

      public static Region enclosingRays(AABB targetBounds, Vec3 explosionPosition) {
         return enclosingRays(targetBounds, explosionPosition, DEFAULT_REGION_PADDING);
      }

      public static Region enclosingRays(AABB targetBounds, Vec3 explosionPosition, int padding) {
         Objects.requireNonNull(targetBounds, "targetBounds");
         Objects.requireNonNull(explosionPosition, "explosionPosition");
         if (padding < 0) {
            throw new IllegalArgumentException("padding must not be negative");
         }

         return new Region(
            floorToBlock(Math.min(targetBounds.minX, explosionPosition.x), -padding),
            floorToBlock(Math.min(targetBounds.minY, explosionPosition.y), -padding),
            floorToBlock(Math.min(targetBounds.minZ, explosionPosition.z), -padding),
            floorToBlock(Math.max(targetBounds.maxX, explosionPosition.x), padding),
            floorToBlock(Math.max(targetBounds.maxY, explosionPosition.y), padding),
            floorToBlock(Math.max(targetBounds.maxZ, explosionPosition.z), padding)
         );
      }

      private static int floorToBlock(double coordinate, int padding) {
         double padded = Math.floor(coordinate) + padding;
         if (!Double.isFinite(padded) || padded < Integer.MIN_VALUE || padded > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Snapshot coordinate is outside block-position range");
         }
         return (int)padded;
      }

      public BlockPos minimum() {
         return new BlockPos(this.minX, this.minY, this.minZ);
      }

      public BlockPos maximum() {
         return new BlockPos(this.maxX, this.maxY, this.maxZ);
      }
   }

   public static ExplosionRaycastSnapshot capture(
      ClientLevel level,
      Entity collisionEntity,
      Region region,
      long worldGeneration,
      long captureTick
   ) {
      return capture(
         level,
         collisionEntity,
         region,
         worldGeneration,
         captureTick,
         DEFAULT_MAX_CAPTURED_BLOCKS
      );
   }

   public static ExplosionRaycastSnapshot capture(
      ClientLevel level,
      Entity collisionEntity,
      Region region,
      long worldGeneration,
      long captureTick,
      int maximumCapturedBlocks
   ) {
      Objects.requireNonNull(level, "level");
      Objects.requireNonNull(collisionEntity, "collisionEntity");
      Objects.requireNonNull(region, "region");
      if (maximumCapturedBlocks < 1) {
         throw new IllegalArgumentException("maximumCapturedBlocks must be positive");
      }

      Minecraft minecraft = Minecraft.getInstance();
      if (!minecraft.isSameThread()) {
         throw new IllegalStateException("Explosion ray snapshots must be captured on the client thread");
      }
      if (minecraft.level != level) {
         throw new IllegalStateException("Cannot capture a snapshot for an inactive client world");
      }
      if (collisionEntity.level() != level) {
         throw new IllegalStateException("Cannot capture collision geometry for an entity from another world");
      }

      long volume;
      try {
         volume = region.volume();
      } catch (ArithmeticException overflow) {
         throw new IllegalArgumentException("Snapshot region volume overflow", overflow);
      }
      if (volume > maximumCapturedBlocks) {
         throw new IllegalArgumentException(
            "Snapshot region requires " + volume + " blocks; limit is " + maximumCapturedBlocks
         );
      }

      long maximumYExclusive = (long)level.getMinY() + level.getHeight();
      if (region.minY() < level.getMinY() || region.maxY() >= maximumYExclusive) {
         throw new IllegalArgumentException("Snapshot region extends outside the active world's build height");
      }
      int minimumChunkX = Math.floorDiv(region.minX(), 16);
      int maximumChunkX = Math.floorDiv(region.maxX(), 16);
      int minimumChunkZ = Math.floorDiv(region.minZ(), 16);
      int maximumChunkZ = Math.floorDiv(region.maxZ(), 16);
      for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; chunkX++) {
         for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; chunkZ++) {
            if (!level.hasChunk(chunkX, chunkZ)) {
               throw new IllegalStateException("Snapshot region includes an unloaded chunk");
            }
         }
      }

      CollisionContext collisionContext = CollisionContext.of(collisionEntity);
      Map<Long, ExplosionRaycastSnapshot.BlockGeometry> capturedBlocks = new HashMap<>((int)volume);
      BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();

      for (long x = region.minX(); x <= (long)region.maxX(); x++) {
         for (long y = region.minY(); y <= (long)region.maxY(); y++) {
            for (long z = region.minZ(); z <= (long)region.maxZ(); z++) {
               position.set((int)x, (int)y, (int)z);
               BlockState state = level.getBlockState(position);
               VoxelShape collisionShape = state.getCollisionShape(level, position, collisionContext);
               float explosionResistance = state.getBlock().getExplosionResistance();

               capturedBlocks.put(
                  position.asLong(),
                  new ExplosionRaycastSnapshot.BlockGeometry(
                     explosionResistance,
                     collisionShape.toAabbs()
                  )
               );
            }
         }
      }

      String dimensionId = level.dimension().identifier().toString();
      return new ExplosionRaycastSnapshot(
         worldGeneration,
         captureTick,
         dimensionId,
         capturedBlocks
      );
   }
}
