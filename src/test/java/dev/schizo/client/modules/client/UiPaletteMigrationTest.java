package dev.schizo.client.modules.client;

import java.awt.Color;
import dev.schizo.client.modules.client.UIModule.PaletteChoice;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UiPaletteMigrationTest {
   @Test void migratesOnlyCompleteLegacyNamedPalette() {
      var old = UiPaletteMigration.LEGACY_SCHIZO;
      assertEquals(UiPaletteMigration.Action.MIGRATE_LEGACY, UiPaletteMigration.resolve(PaletteChoice.SCHIZO, old));
      var customized = new UiPalette(old.accent(), Color.BLACK, old.button(), old.enabled(), old.tint(), old.gradient());
      assertEquals(UiPaletteMigration.Action.PRESERVE_CUSTOM, UiPaletteMigration.resolve(PaletteChoice.SCHIZO, customized));
      assertEquals(UiPaletteMigration.Action.KEEP, UiPaletteMigration.resolve(PaletteChoice.CUSTOM, old));
   }

   @Test void keepsNamedPalettesAndPreservesMismatchedColors() {
      for (var choice : PaletteChoice.values()) {
         if (choice == PaletteChoice.CUSTOM) continue;
         assertEquals(UiPaletteMigration.Action.KEEP, UiPaletteMigration.resolve(choice, choice.getPalette()));
      }
      assertEquals(UiPaletteMigration.Action.PRESERVE_CUSTOM,
         UiPaletteMigration.resolve(PaletteChoice.MIDNIGHT, PaletteChoice.FROST.getPalette()));
   }
}
