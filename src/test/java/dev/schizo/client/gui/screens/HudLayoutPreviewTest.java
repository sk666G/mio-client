package dev.schizo.client.gui.screens;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudLayoutPreviewTest {
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
