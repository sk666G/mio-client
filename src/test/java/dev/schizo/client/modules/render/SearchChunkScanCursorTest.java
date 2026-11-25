package dev.schizo.client.modules.render;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

final class SearchChunkScanCursorTest {
   @Test
   void followsSearchLoopOrderAcrossWholeChunk() {
      SearchChunkScanCursor cursor =
         new SearchChunkScanCursor(-32, 48, 5, 8);
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

      int count = 0;
      long first = Long.MIN_VALUE;
      long sixteenth = Long.MIN_VALUE;
      long seventeenth = Long.MIN_VALUE;
      long last = Long.MIN_VALUE;
      while (cursor.next(pos)) {
         if (count == 0) first = pos.asLong();
         if (count == 15) sixteenth = pos.asLong();
         if (count == 16) seventeenth = pos.asLong();
         last = pos.asLong();
         count++;
      }

      assertEquals(16 * 16 * 3, count);
      assertEquals(BlockPos.asLong(-32, 5, 48), first);
      assertEquals(BlockPos.asLong(-32, 5, 63), sixteenth);
      assertEquals(BlockPos.asLong(-32, 6, 48), seventeenth);
      assertEquals(BlockPos.asLong(-17, 7, 63), last);
      assertFalse(cursor.next(pos));
   }

   @Test
   void emptyHeightRangeCompletesImmediately() {
      SearchChunkScanCursor cursor =
         new SearchChunkScanCursor(0, 0, 9, 9);

      assertFalse(cursor.next(new BlockPos.MutableBlockPos()));
   }
}
