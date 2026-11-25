package dev.schizo.client.modules.render;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

final class TunnelsChunkScanCursorTest {
   @Test
   void visitsEveryCellExactlyOnce() {
      TunnelsModule.ChunkScanCursor cursor =
         new TunnelsModule.ChunkScanCursor(new ChunkPos(2, -3), 10, 13);
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

      int count = 0;
      long first = Long.MIN_VALUE;
      long last = Long.MIN_VALUE;
      while (cursor.next(pos)) {
         if (count == 0) {
            first = pos.asLong();
         }
         last = pos.asLong();
         count++;
      }

      assertEquals(16 * 16 * 3, count);
      assertEquals(BlockPos.asLong(32, 10, -48), first);
      assertEquals(BlockPos.asLong(47, 12, -33), last);
      assertFalse(cursor.next(pos));
   }

   @Test
   void emptyVerticalRangeCompletesImmediately() {
      TunnelsModule.ChunkScanCursor cursor =
         new TunnelsModule.ChunkScanCursor(new ChunkPos(0, 0), 20, 20);

      assertFalse(cursor.next(new BlockPos.MutableBlockPos()));
   }
}
