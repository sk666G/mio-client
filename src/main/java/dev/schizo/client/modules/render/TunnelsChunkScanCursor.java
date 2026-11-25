package dev.schizo.client.modules.render;

import net.minecraft.core.BlockPos;

final class TunnelsChunkScanCursor {
   private final int baseX;
   private final int baseZ;
   private final int firstY;
   private final int endY;
   private int localX;
   private int localZ;
   private int y;

   TunnelsChunkScanCursor(int baseX, int baseZ, int firstY, int endY) {
      this.baseX = baseX;
      this.baseZ = baseZ;
      this.firstY = firstY;
      this.endY = Math.max(firstY, endY);
      this.y = firstY;
   }

   boolean next(BlockPos.MutableBlockPos target) {
      if (this.localX >= 16 || this.firstY >= this.endY) {
         return false;
      }

      target.set(this.baseX + this.localX, this.y, this.baseZ + this.localZ);

      this.y++;
      if (this.y >= this.endY) {
         this.y = this.firstY;
         this.localZ++;
         if (this.localZ >= 16) {
            this.localZ = 0;
            this.localX++;
         }
      }
      return true;
   }
}
