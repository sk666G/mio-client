package dev.schizo.client.modules.render;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class TunnelsChunkScanCursorTest {
   @Test
   void visitsEveryCellExactlyOnce() {
      TunnelsChunkScanCursor cursor =
         new TunnelsChunkScanCursor(32, -48, 10, 13);

      int count = 0;
      TunnelsChunkScanCursor.Cell first = null;
      TunnelsChunkScanCursor.Cell last = null;
      TunnelsChunkScanCursor.Cell cell;
      while ((cell = cursor.next()) != null) {
         if (count == 0) {
            first = cell;
         }
         last = cell;
         count++;
      }

      assertEquals(16 * 16 * 3, count);
      assertEquals(new TunnelsChunkScanCursor.Cell(32, 10, -48), first);
      assertEquals(new TunnelsChunkScanCursor.Cell(47, 12, -33), last);
      assertNull(cursor.next());
   }

   @Test
   void emptyVerticalRangeCompletesImmediately() {
      TunnelsChunkScanCursor cursor =
         new TunnelsChunkScanCursor(0, 0, 20, 20);

      assertNull(cursor.next());
   }
}
