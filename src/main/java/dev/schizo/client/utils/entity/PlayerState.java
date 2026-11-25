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
import dev.schizo.client.utils.MonotonicTime;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface PlayerState {
   void schizo$setRole(SocialRole role);

   boolean schizo$isNextToWall();

   SocialRole schizo$getRole();

   boolean schizo$wasCrawling();

   default float getItemUseProgress(ItemStack stack, LivingEntity entity) {
      long elapsedNanos = MonotonicTime.elapsedNanos(
         schizo$getLastEatingTimeNanos(),
         System.nanoTime()
      );
      double durationNanos = (stack.getUseDuration(entity) * 50.0D + 50.0D) * 1_000_000.0D;
      return (float)(elapsedNanos / durationNanos);
   }

   float schizo$getHealthFactor();

   long schizo$getLastEatingTimeNanos();

   int schizo$getLastAttackedTicksServer();

   void schizo$resetLastAttackedServer();
}
