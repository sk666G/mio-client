package dev.schizo.client.modules.client;

import java.awt.Color;
import dev.schizo.client.modules.client.UIModule.PaletteChoice;

/** Preserve custom colors; migrate only the exact prior named Schizo palette. */
final class UiPaletteMigration {
   enum Action { KEEP, MIGRATE_LEGACY, PRESERVE_CUSTOM }

   static final UiPalette LEGACY_SCHIZO = new UiPalette(
      new Color(190, 255, 64, 220),
      new Color(18, 22, 26, 230),
      new Color(32, 38, 42, 205),
      new Color(190, 255, 64, 120),
      new Color(8, 12, 15, 150),
      new Color(105, 160, 35, 24)
   );

   private UiPaletteMigration() {}

   static Action resolve(PaletteChoice choice, UiPalette actual) {
      if (choice == PaletteChoice.CUSTOM) return Action.KEEP;
      if (choice == PaletteChoice.SCHIZO && LEGACY_SCHIZO.equals(actual)) return Action.MIGRATE_LEGACY;
      return actual.equals(choice.getPalette()) ? Action.KEEP : Action.PRESERVE_CUSTOM;
   }
}
