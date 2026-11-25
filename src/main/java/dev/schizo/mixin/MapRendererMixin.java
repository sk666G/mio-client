/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.schizo.client.ClientContext;
import dev.schizo.client.modules.render.NoRenderModule;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.MapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MapRenderer.class)
public class MapRendererMixin {

   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void suppressMapRendering(
      MapRenderState mapState, PoseStack poses, SubmitNodeCollector collector, boolean ignoredRenderFlag, int packedLight, CallbackInfo callback
   ) {
      if (schizo$noRenderModule().shouldHideMaps()) {
         callback.cancel();
      }
   }

   @org.spongepowered.asm.mixin.Unique
   private static NoRenderModule schizo$noRenderModule() {
      return dev.schizo.client.bootstrap.MixinModuleRegistry.require(NoRenderModule.class);
   }
}
