package dev.schizo.client.modules.client;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UiPaletteTest {
   @Test
   void namedPalettesProvideCompleteDistinctColorSets() {
      Set<Integer> accents = new HashSet<>();
      for (UIModule.PaletteChoice choice : UIModule.PaletteChoice.values()) {
         if (choice == UIModule.PaletteChoice.CUSTOM) {
            assertNull(choice.getPalette());
            continue;
         }

         UiPalette palette = choice.getPalette();
         assertNotNull(palette);
         assertNotNull(palette.accent());
         assertNotNull(palette.background());
         assertNotNull(palette.button());
         assertNotNull(palette.enabled());
         assertNotNull(palette.tint());
         assertNotNull(palette.gradient());
         assertTrue(accents.add(palette.accent().getRGB()), "Palette accents should be distinct");
      }
   }

   @Test
   void schizoPaletteUsesCrimsonAndGraphite() {
      UiPalette palette = UIModule.PaletteChoice.SCHIZO.getPalette();

      assertEquals(new Color(231, 57, 77, 235), palette.accent());
      assertEquals(new Color(19, 20, 26, 250), palette.background());
      assertEquals(new Color(33, 34, 42, 220), palette.button());
   }
}
