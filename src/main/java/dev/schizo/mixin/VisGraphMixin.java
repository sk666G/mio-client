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
import dev.schizo.client.modules.player.FreecamModule;
import dev.schizo.client.modules.render.XrayModule;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VisGraph.class)
public class VisGraphMixin {

   @Inject(method = "setOpaque", at = @At("HEAD"), cancellable = true)
   private void suppressOpaqueBlockMarking(BlockPos blockPos, CallbackInfo callback) {
      if (XrayModule.getInstance()
            .isToggled()
         || schizo$freecam().isToggled()) {
         callback.cancel();
      }
   }

   @org.spongepowered.asm.mixin.Unique
   private static FreecamModule schizo$freecam() {
      return dev.schizo.client.bootstrap.MixinModuleRegistry.require(FreecamModule.class);
   }
}
