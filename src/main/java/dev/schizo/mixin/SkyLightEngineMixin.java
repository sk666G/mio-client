/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.mixin;

import dev.schizo.client.ClientContext;
import dev.schizo.client.modules.render.NoRenderModule;
import net.minecraft.world.level.lighting.SkyLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SkyLightEngine.class)
public class SkyLightEngineMixin {
   private static NoRenderModule noRenderModule() {
      return ClientContext.requireModule(NoRenderModule.class);
   }

   @Inject(at = @At("HEAD"), method = "getLowestSourceY", cancellable = true)
   private void suppressSkyLightSource(
      int chunkX, int chunkZ, int blockY, CallbackInfoReturnable<Integer> callback
   ) {
      if (noRenderModule().isToggled() && noRenderModule().skyLightSetting.getValue()) {
         callback.setReturnValue(15);
         callback.cancel();
      }
   }
}
