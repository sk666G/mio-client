/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.mixin;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.FishingHookPullEntityEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingHook.class)
public class FishingHookMixin implements ClientAccess {
   @Inject(method = "pullEntity", at = @At("HEAD"), cancellable = true)
   private void postFishingHookPullEvent(Entity entity, CallbackInfo callback) {
      FishingHookPullEntityEvent event = new FishingHookPullEntityEvent(entity);
      ClientAccess.eventBus.post(
         event
      );
      if (event.isCancelled()) {
         callback.cancel();
      }
   }
}
