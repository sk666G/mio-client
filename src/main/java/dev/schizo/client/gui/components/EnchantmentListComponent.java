/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.gui.components;

import dev.schizo.client.gui.panels.Panel;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.utils.inventory.EnchantmentUtils;

import java.util.function.Predicate;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

public class EnchantmentListComponent extends AbstractItemListComponent<EnchantmentListEntry> {
   static final ItemStack ENCHANTED_BOOK_ICON;

   public EnchantmentListComponent(Panel panel, Predicate<EnchantmentListEntry> filter) {
      super(
         panel,
         filter,256);
   }

   @Override
   public void update(double mouseX, double mouseY) {
      this.entries.clear();
      if (ClientAccess.schizoMinecraft.level == null) {
         return;
      }
      Registry<Enchantment> registry = ClientAccess.schizoMinecraft.level
         .registryAccess()
         .lookupOrThrow(Registries.ENCHANTMENT);

      for (ResourceKey<Enchantment> enchantmentKey : registry.registryKeySet()) {
         Identifier enchantmentId = enchantmentKey.identifier();
         Enchantment enchantment = registry.getValue(enchantmentId);
         int maxLevel = enchantment.getMaxLevel();

         String name = enchantment.description().getString();
         for (int level = 1; level <= maxLevel; level++) {
            this.entries
               .add(new EnchantmentListEntry(enchantmentKey, level, name));
         }
      }

      this.entries
         .removeIf(this.filter.negate());
   }

   @Override
   public String getEntryLabel(EnchantmentListEntry entry) {
      Enchantment enchantment = (Enchantment)EnchantmentUtils.getHolder(entry.enchantmentKey()).value();
      if (enchantment.getMaxLevel() == 1) {
         return entry.name();
      }
      return entry.name() + " " + Component.translatable("enchantment.level." + entry.level()).getString();
   }

   @Override
   public ItemStack getEntryIcon(EnchantmentListEntry unusedEntry) {
      return ENCHANTED_BOOK_ICON;
   }

   static {
      ENCHANTED_BOOK_ICON = Items.ENCHANTED_BOOK
         .getDefaultInstance();
      ENCHANTED_BOOK_ICON.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
   }

}
