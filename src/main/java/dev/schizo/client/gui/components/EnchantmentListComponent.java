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
import net.minecraft.locale.Language;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.function.Predicate;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

public class EnchantmentListComponent extends AbstractItemListComponent<EnchantmentListEntry> {
   static final ItemStack ENCHANTED_BOOK_ICON;
   private Registry<Enchantment> cachedRegistry;
   private Language cachedLanguage;
   private List<EnchantmentListEntry> catalog=List.of();
   private Map<ResourceKey<?>,Boolean> multipleLevels=Map.of();

   public EnchantmentListComponent(Panel panel, Predicate<EnchantmentListEntry> filter) {
      super(
         panel,
         filter,256);
   }

   @Override
   public void update(double mouseX, double mouseY) {
      this.entries.clear();
      if (ClientAccess.schizoMinecraft.level == null) {
         clearCatalog();
         return;
      }
      Registry<Enchantment> registry = ClientAccess.schizoMinecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
      Language language=Language.getInstance();
      if(registry!=cachedRegistry || language!=cachedLanguage) rebuildCatalog(registry,language);
      for(var entry:catalog) if(filter.test(entry)) entries.add(entry);
   }

   private void rebuildCatalog(Registry<Enchantment> registry,Language language) {
      var next=new ArrayList<EnchantmentListEntry>();
      var levels=new HashMap<ResourceKey<?>,Boolean>();
      for(ResourceKey<Enchantment> key:registry.registryKeySet()) {
         Enchantment enchantment=registry.getValue(key.identifier());
         if(enchantment==null) continue;
         int maximum=enchantment.getMaxLevel();
         levels.put(key,maximum>1);
         String name=enchantment.description().getString();
         for(int level=1;level<=maximum;level++) next.add(new EnchantmentListEntry(key,level,name));
      }
      catalog=List.copyOf(next);
      multipleLevels=Map.copyOf(levels);
      cachedRegistry=registry;
      cachedLanguage=language;
   }

   private void clearCatalog() {
      cachedRegistry=null;
      cachedLanguage=null;
      catalog=List.of();
      multipleLevels=Map.of();
   }

   @Override public void cancelInput() {
      clearCatalog();
      entries.clear();
      super.cancelInput();
   }

   @Override
   public String getEntryLabel(EnchantmentListEntry entry) {
      return Boolean.TRUE.equals(multipleLevels.get(entry.enchantmentKey()))
         ? entry.name()+" "+Component.translatable("enchantment.level."+entry.level()).getString()
         : entry.name();
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
