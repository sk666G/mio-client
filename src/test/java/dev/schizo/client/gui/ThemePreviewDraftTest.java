package dev.schizo.client.gui;

import dev.schizo.client.modules.client.UiPalette;
import dev.schizo.client.modules.client.UIModule.PaletteChoice;
import java.awt.Color;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ThemePreviewDraftTest {
   @Test void switchingPreviewsPreservesCustomColorsAndRejectsStaleChanges() {
      var custom = new UiPalette(Color.ORANGE, Color.BLACK, Color.DARK_GRAY, Color.GRAY, Color.BLACK, Color.RED);
      var draft = new ThemePreviewDraft(PaletteChoice.CUSTOM, custom);
      assertEquals(custom, draft.preview());
      draft.select(PaletteChoice.SCHIZO);
      assertEquals(PaletteChoice.SCHIZO.getPalette(), draft.preview());
      assertTrue(draft.canApply(PaletteChoice.CUSTOM, custom));
      assertFalse(draft.canApply(PaletteChoice.CUSTOM, PaletteChoice.FROST.getPalette()));
      assertFalse(draft.canApply(PaletteChoice.FROST, custom));
      draft.select(PaletteChoice.CUSTOM);
      assertEquals(custom, draft.preview());
      assertFalse(draft.canApply(PaletteChoice.CUSTOM, custom));
   }

   @Test void initialNamedPaletteUsesActualColorsInsteadOfDiscardingOverrides() {
      var actual = PaletteChoice.FROST.getPalette();
      var draft = new ThemePreviewDraft(PaletteChoice.SCHIZO, actual);
      assertEquals(actual, draft.preview());
      draft.select(PaletteChoice.MIDNIGHT);
      assertEquals(PaletteChoice.MIDNIGHT.getPalette(), draft.preview());
      draft.select(PaletteChoice.SCHIZO);
      assertEquals(actual, draft.preview());
   }

   @Test void customPreviewIsDetachedAndStaleGuarded() {
      var original = PaletteChoice.SCHIZO.getPalette();
      var custom = new UiPalette(Color.ORANGE, original.background(), original.button(), original.enabled(), original.tint(), original.gradient());
      var draft = new ThemePreviewDraft(PaletteChoice.SCHIZO, original);
      draft.selectCustom(custom);
      assertEquals(PaletteChoice.CUSTOM, draft.selected());
      assertEquals(custom, draft.preview());
      assertTrue(draft.canApply(PaletteChoice.SCHIZO, original));
      assertFalse(draft.canApply(PaletteChoice.SCHIZO, custom));
      draft.select(PaletteChoice.SCHIZO);
      assertEquals(original, draft.preview());
      assertFalse(draft.canApply(PaletteChoice.SCHIZO, original));
   }
}
