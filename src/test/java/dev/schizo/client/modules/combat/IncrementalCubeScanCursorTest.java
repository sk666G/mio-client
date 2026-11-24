package dev.schizo.client.modules.combat;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class IncrementalCubeScanCursorTest {
   @Test
   void preservesOriginalNestedLoopOrder() {
      IncrementalCubeScanCursor cursor = new IncrementalCubeScanCursor(-1, 1);
      List<IncrementalCubeScanCursor.Offset> offsets = new ArrayList<>();
      IncrementalCubeScanCursor.Offset offset;
      while ((offset = cursor.next()) != null) {
         offsets.add(offset);
      }

      assertEquals(List.of(
         new IncrementalCubeScanCursor.Offset(-1, -1, -1),
         new IncrementalCubeScanCursor.Offset(-1, -1, 0),
         new IncrementalCubeScanCursor.Offset(-1, 0, -1),
         new IncrementalCubeScanCursor.Offset(-1, 0, 0),
         new IncrementalCubeScanCursor.Offset(0, -1, -1),
         new IncrementalCubeScanCursor.Offset(0, -1, 0),
         new IncrementalCubeScanCursor.Offset(0, 0, -1),
         new IncrementalCubeScanCursor.Offset(0, 0, 0)
      ), offsets);
      assertTrue(cursor.isComplete());
      assertNull(cursor.next());
   }

   @Test
   void supportsSingleOffsetRange() {
      IncrementalCubeScanCursor cursor = new IncrementalCubeScanCursor(0, 1);
      assertEquals(new IncrementalCubeScanCursor.Offset(0, 0, 0), cursor.next());
      assertTrue(cursor.isComplete());
      assertNull(cursor.next());
   }

   @Test
   void rejectsEmptyRanges() {
      assertThrows(IllegalArgumentException.class, () -> new IncrementalCubeScanCursor(1, 1));
      assertThrows(IllegalArgumentException.class, () -> new IncrementalCubeScanCursor(2, 1));
   }
}
