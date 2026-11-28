package dev.schizo.client.gui.screens;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StudioResizeFocusTest {
   @Test void resolvesNewWidgetForSameRoleAfterRebuild() {
      var focus = new StudioResizeFocus<Object>();
      Object oldSearch = new Object(), newSearch = new Object();
      focus.register("search", oldSearch);
      String role = focus.roleOf(oldSearch);
      focus.clear();
      focus.register("search", newSearch);
      assertSame(newSearch, focus.resolve(role, widget -> true));
      assertNull(focus.roleOf(oldSearch));
   }

   @Test void usesIdentityNotEquality() {
      var focus = new StudioResizeFocus<Object>();
      Object a = new String("same"), b = new String("same");
      focus.register("a", a);
      focus.register("b", b);
      assertEquals("b", focus.roleOf(b));
      assertNull(focus.roleOf(new String("same")));
      assertNull(focus.roleOf(null));
   }

   @Test void missingOrDisabledRoleDoesNotRestore() {
      var focus = new StudioResizeFocus<Object>();
      focus.register("apply", new Object());
      assertNull(focus.resolve("apply", widget -> false));
      focus.clear();
      focus.register("back", new Object());
      assertNull(focus.resolve("apply", widget -> true));
      assertNull(focus.resolve(null, widget -> true));
   }
}
