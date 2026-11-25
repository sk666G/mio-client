package dev.schizo.client.modules.render;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

final class TunnelsChunkScanCursorTest {
   @Test
   void visitsEveryCellExactlyOnce() {
      TunnelsChunkScanCursor cursor =
         new TunnelsChunkScanCursor(32, -48, 10, 13);
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
      TunnelsChunkScanCursor cursor =
         new TunnelsChunkScanCursor(0, 0, 20, 20);

      assertFalse(cursor.next(new BlockPos.MutableBlockPos()));
   }
}
