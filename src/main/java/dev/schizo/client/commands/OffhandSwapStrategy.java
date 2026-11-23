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

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;

public class OffhandSwapStrategy implements ActionSwapStrategy {

   @Override
   public SwapSlots findSlots(Item[] items) {

      return new SwapSlots(InventoryUtils.toMenuSlot(InventoryUtils.findInventorySlot(items)), 40);
   }

   @Override
   public void apply(SwapSlots slots) {
      MultiPlayerGameMode gameMode = ClientAccess.schizoMinecraft.gameMode;
      int containerId = ClientAccess.schizoMinecraft.player.containerMenu.containerId;
      gameMode.handleInventoryMouseClick(containerId, slots.sourceSlot(), 40, ClickType.SWAP, ClientAccess.schizoMinecraft.player);
   }

   @Override
   public void undo(SwapSlots slots) {
      MultiPlayerGameMode gameMode = ClientAccess.schizoMinecraft.gameMode;
      int containerId = ClientAccess.schizoMinecraft.player.containerMenu.containerId;
      gameMode.handleInventoryMouseClick(containerId, slots.sourceSlot(), 40, ClickType.SWAP, ClientAccess.schizoMinecraft.player);
   }

}
