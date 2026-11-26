package dev.schizo.client.utils.inventory;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.SelectedSlotChangeEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;

/** One module's temporary selection, valid only until the next external selection. */
public final class TemporaryHotbarSelection {
   private static final SelectionRevision REVISION = new SelectionRevision();
   private LocalPlayer player;
   private ClientLevel level;
   private int originalSlot;
   private int selectedSlot;
   private long revision;

   static {
      ClientAccess.eventBus.subscribeStatic(TemporaryHotbarSelection.class);
   }

   @EventHandler
   private static void onSlotChange(SelectedSlotChangeEvent event) {
      REVISION.changed();
   }

   public static void selectionChanged() {
      REVISION.changed();
   }

   public void select(int slot) {
      if (slot < 0 || slot >= 9) return;
      LocalPlayer current = ClientAccess.schizoMinecraft.player;
      ClientLevel currentLevel = ClientAccess.schizoMinecraft.level;
      if (current == null || currentLevel == null) return;
      if (!this.ownsSelection()) {
         this.originalSlot = current.getInventory().getSelectedSlot();
      }
      this.player = current;
      this.level = currentLevel;
      InventoryUtils.selectHotbarSlotForPlacement(slot);
      this.selectedSlot = slot;
      this.revision = REVISION.current();
   }

   private boolean ownsSelection() {
      return this.player != null && ClientAccess.schizoMinecraft.player == this.player
         && ClientAccess.schizoMinecraft.level == this.level && REVISION.owns(this.revision)
         && this.player.getInventory().getSelectedSlot() == this.selectedSlot;
   }

   public void restore(boolean requested) {
      try {
         if (requested && this.ownsSelection()) InventoryUtils.selectHotbarSlot(this.originalSlot);
      } finally {
         this.clear();
      }
   }

   public void clear() {
      this.player = null;
      this.level = null;
   }
}
