/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils.entity;

import dev.schizo.client.settings.options.SocialRole;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface PlayerState {
   void schizo$setRole(SocialRole role);

   boolean schizo$isNextToWall();

   SocialRole schizo$getRole();

   boolean schizo$wasCrawling();

   default float getItemUseProgress(ItemStack stack, LivingEntity entity) {
      float elapsedMillis = (float)(System.currentTimeMillis() - schizo$getLastEatingTime());
      return elapsedMillis / (stack.getUseDuration(entity) * 50.0F + 50.0F);
   }

   float schizo$getHealthFactor();

   long schizo$getLastEatingTime();

   int schizo$getLastAttackedTicksServer();

   void schizo$resetLastAttackedServer();
}
