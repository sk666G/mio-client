package dev.schizo.client.gui.screens;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudLayoutPreviewTest {
   private record Layout(String anchor, float size, float derivedX) {}

   @Test
   void renderDerivedGeometryDoesNotRevokePreviewOwnershipOrCreateNoOpHistory() {
      var before = new Layout("right", 1, 100);
      var previewed = new Layout("right", 2, 100);
      java.util.function.BiPredicate<Layout, Layout> editableFields = (a, b) -> a.anchor().equals(b.anchor()) && a.size() == b.size();
      assertEquals(before, new HudLayoutPreview<>(before, previewed, editableFields).cancel(new Layout("right", 2, 80)));
      assertNull(new HudLayoutPreview<>(before, previewed, editableFields).cancel(new Layout("left", 2, 80)));
      var history = new HudLayoutHistory<Layout>(100);
      assertTrue(new HudLayoutPreview<>(before, before, editableFields).commit(new Layout("right", 1, 90), history));
      assertFalse(history.canUndo());
   }

   @Test
   void commitIsOneHistoryEditAndCancelPreservesAnExternalChange() {
      var history = new HudLayoutHistory<String>(100);
      var preview = new HudLayoutPreview<>("before", "preview");
      assertTrue(preview.commit("preview", history));
      assertEquals("before", history.undo("preview"));
      assertNull(preview.cancel("preview"));
      var replaced = new HudLayoutPreview<>("before", "preview");
      assertNull(replaced.cancel("external"));
      assertEquals("before", new HudLayoutPreview<>("before", "preview").cancel("preview"));
   }
}
