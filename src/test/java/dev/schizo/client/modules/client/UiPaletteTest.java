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
   void schizoPaletteKeepsItsElectricLimeIdentity() {
      UiPalette palette = UIModule.PaletteChoice.SCHIZO.getPalette();

      assertEquals(new Color(190, 255, 64, 220), palette.accent());
      assertEquals(new Color(18, 22, 26, 230), palette.background());
      assertEquals(new Color(32, 38, 42, 205), palette.button());
   }
}
