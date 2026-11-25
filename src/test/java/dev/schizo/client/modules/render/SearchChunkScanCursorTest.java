package dev.schizo.client.modules.render;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class SearchChunkScanCursorTest {
   @Test
   void followsSearchLoopOrderAcrossWholeChunk() {
      SearchChunkScanCursor cursor =
         new SearchChunkScanCursor(-32, 48, 5, 8);

      int count = 0;
      SearchChunkScanCursor.Cell first = null;
      SearchChunkScanCursor.Cell sixteenth = null;
      SearchChunkScanCursor.Cell seventeenth = null;
      SearchChunkScanCursor.Cell last = null;
      SearchChunkScanCursor.Cell cell;
      while ((cell = cursor.next()) != null) {
         if (count == 0) first = cell;
         if (count == 15) sixteenth = cell;
         if (count == 16) seventeenth = cell;
         last = cell;
         count++;
      }

      assertEquals(16 * 16 * 3, count);
      assertEquals(new SearchChunkScanCursor.Cell(-32, 5, 48), first);
      assertEquals(new SearchChunkScanCursor.Cell(-32, 5, 63), sixteenth);
      assertEquals(new SearchChunkScanCursor.Cell(-32, 6, 48), seventeenth);
      assertEquals(new SearchChunkScanCursor.Cell(-17, 7, 63), last);
      assertNull(cursor.next());
   }

   @Test
   void emptyHeightRangeCompletesImmediately() {
      SearchChunkScanCursor cursor =
         new SearchChunkScanCursor(0, 0, 9, 9);

      assertNull(cursor.next());
   }
}
