/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.render;

import com.google.common.collect.AbstractIterator;
import java.util.Iterator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Iterates only the floor slice that can belong to WorldUtils' block sphere.
 * This preserves the existing integer block-center sphere semantics without
 * visiting every Y layer that VoidESP will immediately reject.
 */
final class VoidFloorScanCursor extends AbstractIterator<BlockPos> {
   private final BlockPos center;
   private final int floorY;
   private final float radius;
   private final int extent;
   private int offsetX;
   private int offsetZ;

   VoidFloorScanCursor(Vec3 center, int floorY, float radius) {
      this.center = BlockPos.containing(center);
      this.floorY = floorY;
      this.radius = radius;
      this.extent = (int)Math.ceil(radius);
      this.offsetX = -this.extent;
      this.offsetZ = -this.extent;
   }

   static Iterator<BlockPos> iterator(Vec3 center, int floorY, float radius) {
      return new VoidFloorScanCursor(center, floorY, radius);
   }

   @Override
   protected BlockPos computeNext() {
      while (this.offsetX <= this.extent) {
         while (this.offsetZ <= this.extent) {
            BlockPos position = new BlockPos(
               this.center.getX() + this.offsetX,
               this.floorY,
               this.center.getZ() + this.offsetZ
            );
            this.offsetZ++;
            if (this.center.closerThan(position, this.radius)) {
               return position;
            }
         }

         this.offsetZ = -this.extent;
         this.offsetX++;
      }

      return this.endOfData();
   }
}
