/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.commands;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.utils.inventory.InventoryUtils;

import net.minecraft.world.item.Item;

public class HotbarSwapStrategy implements ActionSwapStrategy {

   @Override
   public SwapSlots findSlots(Item[] items) {
      int sourceSlot = InventoryUtils.findHotbarSlot(items);

      return new SwapSlots(sourceSlot, ClientAccess.schizoMinecraft.player.getInventory().getSelectedSlot());
   }

   @Override
   public void apply(SwapSlots slots) {
      InventoryUtils.selectHotbarSlot(slots.sourceSlot());
   }

   @Override
   public void undo(SwapSlots slots) {
      InventoryUtils.selectHotbarSlot(slots.targetSlot());
   }

}
