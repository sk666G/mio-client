package dev.schizo.client.modules.render;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

final class SearchChunkScanCursor {
   final ChunkPos chunkPosition;
   private final int firstY;
   private final int endY;
   private int localX;
   private int localZ;
   private int y;

   SearchChunkScanCursor(ChunkPos chunkPosition, int firstY, int endY) {
      this.chunkPosition = chunkPosition;
      this.firstY = firstY;
      this.endY = Math.max(firstY, endY);
      this.y = firstY;
   }

   boolean next(BlockPos.MutableBlockPos target) {
      if (this.localX >= 16 || this.firstY >= this.endY) {
         return false;
      }

      int x = this.chunkPosition.getMinBlockX() + this.localX;
      int z = this.chunkPosition.getMinBlockZ() + this.localZ;
      target.set(x, this.y, z);

      this.localZ++;
      if (this.localZ >= 16) {
         this.localZ = 0;
         this.y++;
         if (this.y >= this.endY) {
            this.y = this.firstY;
            this.localX++;
         }
      }
      return true;
   }
}
