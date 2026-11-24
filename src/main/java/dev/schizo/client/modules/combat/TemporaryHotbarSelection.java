package dev.schizo.client.modules.combat;

/** Tracks one temporary hotbar selection without overwriting a later external selection. */
final class TemporaryHotbarSelection {
   private int previousSlot = -1;
   private int ownedSlot = -1;

   void acquire(int previousSlot, int ownedSlot) {
      this.previousSlot = previousSlot;
      this.ownedSlot = ownedSlot;
   }

   boolean isActive() {
      return this.previousSlot >= 0 && this.ownedSlot >= 0;
   }

   int release(int currentSlot) {
      int restoreSlot = currentSlot == this.ownedSlot ? this.previousSlot : -1;
      this.clear();
      return restoreSlot;
   }

   void clear() {
      this.previousSlot = -1;
      this.ownedSlot = -1;
   }
}
