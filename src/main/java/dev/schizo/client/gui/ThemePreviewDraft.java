package dev.schizo.client.gui;

import dev.schizo.client.modules.client.UiPalette;
import dev.schizo.client.modules.client.UIModule.PaletteChoice;
import java.util.Objects;

/** Preview state is deliberately detached from the live UI module. */
public final class ThemePreviewDraft {
   private final PaletteChoice originalChoice;
   private final UiPalette originalColors;
   private PaletteChoice selected;

   public ThemePreviewDraft(PaletteChoice choice, UiPalette colors) {
      originalChoice = Objects.requireNonNull(choice);
      originalColors = Objects.requireNonNull(colors);
      selected = choice;
   }

   public void select(PaletteChoice choice) {
      selected = Objects.requireNonNull(choice);
   }

   public PaletteChoice selected() { return selected; }

   public UiPalette preview() {
      if (selected == originalChoice || selected == PaletteChoice.CUSTOM) return originalColors;
      return selected.getPalette();
   }

   public boolean canApply(PaletteChoice liveChoice, UiPalette liveColors) {
      return selected != originalChoice && selected != PaletteChoice.CUSTOM
         && liveChoice == originalChoice && originalColors.equals(liveColors);
   }

   public boolean isStale(PaletteChoice liveChoice, UiPalette liveColors) {
      return liveChoice != originalChoice || !originalColors.equals(liveColors);
   }
}
