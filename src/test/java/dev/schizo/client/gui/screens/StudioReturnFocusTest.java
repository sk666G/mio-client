package dev.schizo.client.gui.screens;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StudioReturnFocusTest {
   @Test void consumesStateOnlyOnce() {
      var focus = new StudioReturnFocus<Object,String>();
      Object parent = new Object();
      focus.remember(parent, "search");
      assertEquals("search", focus.take(parent));
      assertNull(focus.take(parent));
   }

   @Test void rejectsDifferentParentEvenWhenEqualAndDiscardsOldState() {
      var focus = new StudioReturnFocus<Object,String>();
      Object parent = new String("parent");
      focus.remember(parent, "search");
      assertNull(focus.take(new String("parent")));
      assertNull(focus.take(parent));
   }

   @Test void nestedChildrenRetainIndependentReturnState() {
      var first = new StudioReturnFocus<Object,String>();
      var second = new StudioReturnFocus<Object,String>();
      Object root = new Object(), child = new Object();
      first.remember(root, "search");
      second.remember(child, "open");
      assertEquals("open", second.take(child));
      assertEquals("search", first.take(root));
   }

   @Test void newVisitReplacesPreviousState() {
      var focus = new StudioReturnFocus<Object,String>();
      Object parent = new Object();
      focus.remember(parent, "search");
      focus.remember(parent, "inspect");
      assertEquals("inspect", focus.take(parent));
   }
}
