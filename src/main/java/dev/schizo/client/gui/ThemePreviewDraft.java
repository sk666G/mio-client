package dev.schizo.client.gui;

import dev.schizo.client.modules.client.UiPalette;
import dev.schizo.client.modules.client.UIModule.PaletteChoice;
import java.util.Objects;

/** Preview state is deliberately detached from the live UI module. */
public final class ThemePreviewDraft {
   private final PaletteChoice originalChoice;
   private final UiPalette originalColors;
   private PaletteChoice selected;
   private UiPalette customPreview;

   public ThemePreviewDraft(PaletteChoice choice, UiPalette colors) {
      originalChoice = Objects.requireNonNull(choice);
      originalColors = Objects.requireNonNull(colors);
      selected = choice;
   }

   public void select(PaletteChoice choice) {
      selected = Objects.requireNonNull(choice);
      customPreview = null;
   }

   public PaletteChoice selected() { return selected; }

   public void selectCustom(UiPalette colors) {
      customPreview = Objects.requireNonNull(colors);
      selected = PaletteChoice.CUSTOM;
   }

   public UiPalette preview() {
      if (customPreview != null) return customPreview;
      if (selected == originalChoice || selected == PaletteChoice.CUSTOM) return originalColors;
      return selected.getPalette();
   }

   public boolean canApply(PaletteChoice liveChoice, UiPalette liveColors) {
      if (isStale(liveChoice, liveColors)) return false;
      if (customPreview != null) return !customPreview.equals(originalColors) || originalChoice != PaletteChoice.CUSTOM;
      return selected != originalChoice && selected != PaletteChoice.CUSTOM;
   }

   public boolean applyCustom(dev.schizo.client.modules.client.UIModule ui) {
      return customPreview != null && canApply(ui.paletteSetting.getValue(), ui.snapshotPalette())
         && ui.applyCustomPalette(originalChoice, originalColors, customPreview);
   }

   public boolean isStale(PaletteChoice liveChoice, UiPalette liveColors) {
      return liveChoice != originalChoice || !originalColors.equals(liveColors);
   }
}
