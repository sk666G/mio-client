package dev.schizo.client.gui.screens;

/** One-shot return state tied to the exact parent instance. */
final class StudioReturnFocus<P,S> {
   private P parent;
   private S state;

   void remember(P parent, S state) {
      this.parent = parent;
      this.state = state;
   }

   S take(P expectedParent) {
      S result = parent == expectedParent ? state : null;
      parent = null;
      state = null;
      return result;
   }
}
