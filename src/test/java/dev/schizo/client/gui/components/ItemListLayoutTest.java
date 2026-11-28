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

   @Test void boundedFittingRetainsLegacyTrimAndRejectsInvalidWidth() {
      assertEquals("A...",ItemListLayout.fitLabel("A   long",4,String::length));
      assertEquals("",ItemListLayout.fitLabel("A",Float.NaN,String::length));
      var count=new java.util.concurrent.atomic.AtomicInteger();
      String result=ItemListLayout.fitLabel("x".repeat(10000),20,text->{count.incrementAndGet();return text.length();});
      assertEquals(20,result.length());
      org.junit.jupiter.api.Assertions.assertTrue(count.get()<=17);
   }
}
