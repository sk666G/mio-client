/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.settings.options;

import dev.schizo.client.ClientContext;
import dev.schizo.client.utils.inventory.EnchantmentUtils;
import dev.schizo.client.utils.entity.PlayerStateTracker;
import dev.schizo.client.utils.entity.PlayerStateUtils;
import dev.schizo.client.settings.NamedEntry;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

public enum AuraMaceChoice implements NamedEntry {
   NONE("None", stack -> false),
   BREACH("Breach", stack -> {
      ResourceKey<Enchantment> enchantment = Enchantments.BREACH;
      return EnchantmentUtils.hasEnchantment(enchantment, stack);
   }),
   DENSITY("Density", stack -> {
      ResourceKey<Enchantment> enchantment = Enchantments.DENSITY;
      return EnchantmentUtils.hasEnchantment(enchantment, stack) && PlayerStateUtils.getTrackedFallDistance() > 3.0F;
   }),
   SMART("Smart", stack -> {
      if (!(PlayerStateUtils.getTrackedFallDistance() > 3.0F)) {
         PlayerStateTracker playerState = ClientContext.playerStateTracker;
         if (playerState.getGroundedTicks() > 1) {
            ResourceKey<Enchantment> enchantment = Enchantments.BREACH;
            return EnchantmentUtils.hasEnchantment(enchantment, stack);
         }
      }
      ResourceKey<Enchantment> enchantment = Enchantments.DENSITY;
      return EnchantmentUtils.hasEnchantment(enchantment, stack);
   });

   private final Predicate<ItemStack> predicate;
   private final String name;

   AuraMaceChoice(String name, Predicate<ItemStack> predicate) {
      this.predicate = predicate;
      this.name = name;
   }

   @Override
   public String getName() {
      return this.name;
   }

   public boolean matches(ItemStack stack) {
      return this.predicate.test(stack);
   }
}
