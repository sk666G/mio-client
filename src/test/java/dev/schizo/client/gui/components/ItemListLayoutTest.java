package dev.schizo.client.gui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class ItemListLayoutTest {
   @Test
   void emptyRowsAndOutsideClicksHaveNoSelection() {
      assertEquals(-1, ItemListLayout.entryAt(16, 4, 5));
      assertEquals(4, ItemListLayout.entryAt(15.99, 4, 5));
      assertEquals(-1, ItemListLayout.entryAt(-0.5, 0, 10));
      assertEquals(-1, ItemListLayout.entryAt(128, 0, 10));
   }

   @Test
   void truncationKeepsWholeCodePointsAndFitsTheAvailableWidth() {
      assertEquals("", ItemListLayout.fitLabel("hello", 2, String::length));
      assertEquals("hello", ItemListLayout.fitLabel("hello", 5, String::length));
      assertEquals("A...", ItemListLayout.fitLabel("A😀long", 5, String::length));
      assertEquals("A😀...", ItemListLayout.fitLabel("A😀long", 6, String::length));
   }
}
