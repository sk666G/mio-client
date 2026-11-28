/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.config;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

public final class PresetUndoState {
   private static final int HISTORY_LIMIT = 5;
   private record Entry(PresetUndoAction action,PresetUndoAction.Guard guard) {}
   private final Deque<Entry> actions = new ArrayDeque<>();

   public void push(PresetUndoAction action) {
      if (action == null) {
         return;
      }
      this.actions.addLast(new Entry(action,action.guardAfterSuccess()));
      while (this.actions.size() > HISTORY_LIMIT) {
         this.actions.removeFirst();
      }
   }

   public PresetUndoAction undo() throws IOException {
      Entry entry=this.actions.peekLast();
      if(entry==null) return null;
      entry.guard().verify();
      entry.action().undo();
      this.actions.removeLast();
      return entry.action();
   }
}
