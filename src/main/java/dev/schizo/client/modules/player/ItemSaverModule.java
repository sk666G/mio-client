/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.player;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.EventHandler;
import dev.schizo.client.events.MultiPlayerGameModeStartDestroyBlockEvent;
import dev.schizo.client.utils.inventory.InventoryUtils;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ItemSaverModule extends Module {
   public static boolean isSafeToUse(ItemStack stack) {
      if (stack.isEmpty() || !stack.isDamageableItem() || stack.has(DataComponents.EQUIPPABLE) || stack.is(Items.ELYTRA)) {
         return true;
      }
      return InventoryUtils.getDurabilityPercent(stack) > 5.0F;
   }

   @EventHandler
   private void onMultiPlayerGameModeStartDestroyBlockEvent(MultiPlayerGameModeStartDestroyBlockEvent event) {
      Inventory inventory = ClientAccess.schizoMinecraft.player.getInventory();
      int selectedSlot = inventory.getSelectedSlot();
      if (!isSafeToUse(inventory.getItem(selectedSlot))) {
         InventoryUtils.selectHotbarSlotForPlacement(selectedSlot == 8 ? selectedSlot - 1 : selectedSlot + 1);
         event.cancel();
      }
   }

   public ItemSaverModule() {
      super("ItemSaver", "Prevents your tools from breaking.", ModuleCategory.PLAYER, new String[0]);
   }
}
