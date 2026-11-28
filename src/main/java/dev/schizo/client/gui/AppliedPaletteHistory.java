package dev.schizo.client.gui;

import dev.schizo.client.modules.client.UiPalette;
import dev.schizo.client.modules.client.UIModule.PaletteChoice;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.Predicate;

/** Session-local fixed-palette history. Restores are conditional on exact current state. */
public final class AppliedPaletteHistory {
   private static final int LIMIT = 16;
   public record State(PaletteChoice choice, UiPalette colors) {
      public State { Objects.requireNonNull(choice); Objects.requireNonNull(colors); }
   }
   private record Edit(State before, State after) {}
   private final Deque<Edit> undo = new ArrayDeque<>();
   private final Deque<Edit> redo = new ArrayDeque<>();

   public void clear() { undo.clear(); redo.clear(); }

   public void invalidateIfStale(State current) {
      if ((!undo.isEmpty() && !undo.peekLast().after().equals(current))
         || (!redo.isEmpty() && !redo.peekLast().before().equals(current))) clear();
   }

   public void record(State before, State after) {
      invalidateIfStale(before);
      if (before.equals(after)) return;
      undo.addLast(new Edit(before, after));
      if (undo.size() > LIMIT) undo.removeFirst();
      redo.clear();
   }

   public boolean canMove(State current, boolean forward) {
      Edit edit = (forward ? redo : undo).peekLast();
      return edit != null && current.equals(forward ? edit.before() : edit.after());
   }

   public boolean move(State current, boolean forward, Predicate<State> restore) {
      invalidateIfStale(current);
      if (!canMove(current, forward)) return false;
      Deque<Edit> source = forward ? redo : undo;
      Deque<Edit> destination = forward ? undo : redo;
      Edit edit = source.peekLast();
      if (!restore.test(forward ? edit.after() : edit.before())) return false;
      source.removeLast();
      destination.addLast(edit);
      return true;
   }
}
