/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.combat;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.utils.inventory.InventoryUtils;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.exploit.XCarryModule;
import dev.schizo.client.settings.NamedEntry;

import java.util.Set;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum OffhandItemChoice implements ClientAccess, NamedEntry {
   Totem("Totem", Items.TOTEM_OF_UNDYING),
   Crystal("Crystal", Items.END_CRYSTAL),
   Gapple("Gapple", Items.GOLDEN_APPLE) {
      @Override
      public Item getItem(boolean useCrapple) {
         return useCrapple ? super.getItem(true) : Items.ENCHANTED_GOLDEN_APPLE;
      }
   },
   Custom("Custom", Items.SHIELD) {
      @Override
      public Item getItem(boolean useCrapple) {
         return (OffhandItemChoice.schizo$offhandModule().customSetting.getValue()).stream().findAny().orElse(null);
      }
   };


   private final String displayName;
   private final Item defaultItem;

   @Override
   public String getName() {
      return this.displayName;
   }

   OffhandItemChoice(String displayName, Item defaultItem) {
      this.displayName = displayName;
      this.defaultItem = defaultItem;
   }

   public Item getItem(boolean useCrapple) {
      return this.defaultItem;
   }

   public int findContainerSlot(boolean useCrapple) {
      Item itemToFind = this.getItem(useCrapple);
      if (itemToFind == null) return -1;

      if (ClientAccess.schizoMinecraft.player.getOffhandItem().is(itemToFind)) return -1;
      if (ClientAccess.schizoMinecraft.screen instanceof ShulkerBoxScreen || ClientAccess.schizoMinecraft.screen instanceof ContainerScreen) {
         NonNullList<Slot> containerSlots = ClientAccess.schizoMinecraft.player.containerMenu.slots;
         for (Slot slot : containerSlots) {
            ItemStack slotStack = slot.getItem();
            if (slotStack.is(itemToFind)) return slot.index;
         }
      }
      boolean includeXCarry = schizo$xCarryModule().isToggled();
      return InventoryUtils.findMenuSlot(itemToFind, includeXCarry);
   }

   public static void bindModuleRegistry(ModuleRegistry modules) {
      schizo$xCarryModule() = (XCarryModule)modules.getModule(
         XCarryModule.class
      );
      schizo$offhandModule() = (OffhandModule)modules.getModule(
         OffhandModule.class
      );
   }

   private static XCarryModule schizo$xCarryModule() {
      return ClientContext.requireModule(XCarryModule.class);
   }

   private static OffhandModule schizo$offhandModule() {
      return ClientContext.requireModule(OffhandModule.class);
   }
}
