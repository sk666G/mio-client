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
import dev.schizo.client.modules.render.ESPModule;
import dev.schizo.client.modules.render.NoRenderModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {


   @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
   private void suppressBlockEntitySubmission(
      BlockEntityRenderState renderState, PoseStack poses, SubmitNodeCollector collector, CameraRenderState cameraState, CallbackInfo callback
   ) {
      if (!schizo$espModule().isRenderingBlockEntities()) {
         if (schizo$noRenderModule().shouldHideBlockEntityType(
            renderState.blockEntityType
         )) {
            callback.cancel();
         } else {
            if (schizo$noRenderModule().isToggled()
               && schizo$noRenderModule().tileEntitiesSetting.getValue()
               && renderState.blockPos.distSqr(Minecraft.getInstance().gameRenderer.getMainCamera().blockPosition())
                  > schizo$noRenderModule().tileDistanceSetting
                        .getValue()
                     * schizo$noRenderModule().tileDistanceSetting
                        .getValue()) {
               callback.cancel();
            }
         }
      }
   }


   @org.spongepowered.asm.mixin.Unique
   private static ESPModule schizo$espModule() {
      return dev.schizo.client.bootstrap.MixinModuleRegistry.require(ESPModule.class);
   }

   @org.spongepowered.asm.mixin.Unique
   private static NoRenderModule schizo$noRenderModule() {
      return dev.schizo.client.bootstrap.MixinModuleRegistry.require(NoRenderModule.class);
   }
}
