package dev.schizo.mixin;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.utils.inventory.TemporaryHotbarSelection;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
public class InventorySelectionMixin {
   @Inject(method = "setSelectedSlot", at = @At("HEAD"))
   private void invalidateTemporarySelection(int slot, CallbackInfo callback) {
      var player = ClientAccess.schizoMinecraft.player;
      if (player != null && player.getInventory() == (Object)this) {
         TemporaryHotbarSelection.selectionChanged();
      }
   }
}
