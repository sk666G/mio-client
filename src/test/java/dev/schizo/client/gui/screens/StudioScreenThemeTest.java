package dev.schizo.client.gui.screens;

import dev.schizo.client.modules.client.UiPalette;
import dev.schizo.client.modules.client.UIModule.PaletteChoice;
import java.awt.Color;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StudioScreenThemeTest {
   @Test void namedPalettesResolveToDistinctChrome() {
      var red = StudioScreenTheme.from(PaletteChoice.SCHIZO.getPalette());
      var blue = StudioScreenTheme.from(PaletteChoice.MIDNIGHT.getPalette());
      assertNotEquals(red.accent(), blue.accent());
      assertNotEquals(red.panel(), blue.panel());
      assertTrue(StudioScreenTheme.contrast(red.accent(), red.panel()) >= 4.5);
      assertTrue(StudioScreenTheme.contrast(blue.accent(), blue.panel()) >= 4.5);
   }

   @Test void darkAccentAndBrightCustomSurfaceRemainReadable() {
      var palette = new UiPalette(Color.BLACK, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE);
      var theme = StudioScreenTheme.from(palette);
      assertTrue(StudioScreenTheme.contrast(theme.accent(), theme.panel()) >= 4.5);
      assertEquals(Color.BLACK, palette.accent());
      assertEquals(Color.WHITE, palette.background());
      assertEquals(255, theme.panel() >>> 24);
   }
}
