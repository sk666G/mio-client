/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import dev.schizo.client.utils.world.HoleInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.phys.AABB;

/** One client-thread traversal. Only a completed traversal is published. */
final class HoleScanTask {
   private static final int RADIUS = 16;
   private static final int WIDTH = 2 * RADIUS;
   private static final int VOLUME = WIDTH * WIDTH * WIDTH;
   private static final int CELLS_PER_TICK = 2048;
   private static final long SLICE_NANOS = 2_000_000L;
   private final BlockPos origin;
   private final MutableBlockPos cursor = new MutableBlockPos();
   private final List<HoleInfo> holes = new ArrayList<>();
   private final Set<Long> coveredPositions = new HashSet<>();
   private int index;

   HoleScanTask(BlockPos origin) {
      this.origin = origin.immutable();
   }

   boolean covers(BlockPos position) {
      return Math.abs((long)position.getX() - this.origin.getX()) < RADIUS
         && Math.abs((long)position.getY() - this.origin.getY()) < RADIUS
         && Math.abs((long)position.getZ() - this.origin.getZ()) < RADIUS;
   }

   boolean advance() {
      long start = System.nanoTime();
      int stop = Math.min(this.index + CELLS_PER_TICK, VOLUME);
      while (this.index < stop) {
         int offset = this.index++;
         this.cursor.set(this.origin.getX() - RADIUS + offset / (WIDTH * WIDTH),
            this.origin.getY() - RADIUS + offset / WIDTH % WIDTH,
            this.origin.getZ() - RADIUS + offset % WIDTH);
         if (!this.coveredPositions.contains(this.cursor.asLong())) {
            HoleInfo hole = HoleManager.classifyHole(this.cursor, null);
            if (hole != null) {
               this.holes.add(hole);
               this.addCoveredPositions(this.coveredPositions, hole.bounds());
            }
         }
         if ((this.index & 31) == 0 && System.nanoTime() - start >= SLICE_NANOS) {
            break;
         }
      }
      return this.index == VOLUME;
   }

   List<HoleInfo> result() {
      return List.copyOf(this.holes);
   }

   private void addCoveredPositions(Set<Long> positions, AABB bounds) {
      for (int x = (int)bounds.minX; x < (int)bounds.maxX; x++) {
         for (int z = (int)bounds.minZ; z < (int)bounds.maxZ; z++) {
            int y = (int)bounds.minY;
            long floorPosition = BlockPos.asLong(x, y, z);
            positions.add(floorPosition);
            positions.add(BlockPos.asLong(x, (int)bounds.minY + 1, z));
            int topY = (int)bounds.minY;
            topY += 2;
            positions.add(BlockPos.asLong(x, topY, z));
         }
      }
   }
}
