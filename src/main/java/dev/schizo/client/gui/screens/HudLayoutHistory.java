/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.gui.screens;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/** A bounded undo/redo history for immutable layout snapshots. */
final class HudLayoutHistory<T> {
   private final int capacity;
   private T pending;
   private final Deque<T> undoStates = new ArrayDeque<>();
   private final Deque<T> redoStates = new ArrayDeque<>();

   HudLayoutHistory(int capacity) {
      if (capacity < 1) {
         throw new IllegalArgumentException("History capacity must be positive");
      }
      this.capacity = capacity;
   }

   boolean hasPendingEdit() {
      return this.pending != null;
   }

   void beginEdit(T before) {
      if (this.pending == null) {
         this.pending = Objects.requireNonNull(before);
      }
   }

   void finishEdit(T after) {
      if (this.pending != null && !this.pending.equals(after)) {
         this.record(this.pending);
      }
      this.pending = null;
   }

   T cancelEdit() {
      T before = this.pending;
      this.pending = null;
      return before;
   }

   void record(T previousState) {
      this.pushBounded(this.undoStates, Objects.requireNonNull(previousState, "previousState"));
      this.redoStates.clear();
   }

   T undo(T currentState) {
      if (this.undoStates.isEmpty()) {
         return null;
      }
      this.pushBounded(this.redoStates, Objects.requireNonNull(currentState, "currentState"));
      return this.undoStates.removeLast();
   }

   T redo(T currentState) {
      if (this.redoStates.isEmpty()) {
         return null;
      }
      this.pushBounded(this.undoStates, Objects.requireNonNull(currentState, "currentState"));
      return this.redoStates.removeLast();
   }

   boolean canUndo() {
      return !this.undoStates.isEmpty();
   }

   boolean canRedo() {
      return !this.redoStates.isEmpty();
   }

   void clear() {
      this.pending = null;
      this.undoStates.clear();
      this.redoStates.clear();
   }

   private void pushBounded(Deque<T> states, T state) {
      states.addLast(state);
      while (states.size() > this.capacity) {
         states.removeFirst();
      }
   }
}
