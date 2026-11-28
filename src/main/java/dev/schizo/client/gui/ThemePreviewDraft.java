package dev.schizo.client.gui;

import dev.schizo.client.modules.client.UiPalette;
import dev.schizo.client.modules.client.UIModule.PaletteChoice;
import java.util.Objects;
import java.util.ArrayDeque;
import java.util.Deque;

/** Preview state is deliberately detached from the live UI module. */
public final class ThemePreviewDraft {
   private final PaletteChoice originalChoice;
   private final UiPalette originalColors;
   private PaletteChoice selected;
   private UiPalette customPreview;
   private static final int HISTORY_LIMIT = 32;
   private record State(PaletteChoice selected, UiPalette custom) {}
   private final Deque<State> undo = new ArrayDeque<>();
   private final Deque<State> redo = new ArrayDeque<>();

   private State state() { return new State(selected, customPreview); }

   private void restore(State state) {
      selected = state.selected();
      customPreview = state.custom();
   }

   private static void push(Deque<State> history, State state) {
      history.addLast(state);
      if (history.size() > HISTORY_LIMIT) history.removeFirst();
   }

   private void change(State next) {
      State before = state();
      if (before.equals(next)) return;
      push(undo, before);
      redo.clear();
      restore(next);
   }

   public boolean canUndo() { return !undo.isEmpty(); }
   public boolean canRedo() { return !redo.isEmpty(); }

   public boolean undo() {
      if (!canUndo()) return false;
      push(redo, state());
      restore(undo.removeLast());
      return true;
   }

   public boolean redo() {
      if (!canRedo()) return false;
      push(undo, state());
      restore(redo.removeLast());
      return true;
   }

   public ThemePreviewDraft(PaletteChoice choice, UiPalette colors) {
      originalChoice = Objects.requireNonNull(choice);
      originalColors = Objects.requireNonNull(colors);
      selected = choice;
   }

   public void select(PaletteChoice choice) {
      change(new State(Objects.requireNonNull(choice), null));
   }

   public PaletteChoice selected() { return selected; }

   public void selectCustom(UiPalette colors) {
      change(new State(PaletteChoice.CUSTOM, Objects.requireNonNull(colors)));
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
