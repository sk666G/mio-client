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
import dev.schizo.client.render.RenderUtils;
import dev.schizo.client.modules.render.NoRenderModule;
import dev.schizo.client.modules.render.XrayModule;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemBlockRenderTypes.class)
public class ItemBlockRenderTypesMixin {


   @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
   private static void applyTranslucentHandRenderType(
      BlockState blockState, CallbackInfoReturnable<RenderType> callback
   ) {
      if (schizo$noRenderModule().getHandOpacity()
            != 1.0F
         && RenderUtils.getRenderColor()[3]
            != 255) {
         callback.setReturnValue(Sheets.translucentItemSheet());
      }
   }

   @Inject(method = "getChunkRenderType", at = @At("HEAD"), cancellable = true)
   private static void applyXrayTranslucentChunkLayer(
      BlockState blockState, CallbackInfoReturnable<ChunkSectionLayer> callback
   ) {
      if (schizo$xrayModule().isToggled()
         && !schizo$xrayModule().alphaSetting
            .isAtMinimum()
         && !schizo$xrayModule().alphaSetting
            .isAtMaximum()
         && !schizo$xrayModule().shouldRenderBlock(
            BlockPos.ZERO,
            blockState.getBlock()
         )) {
         callback.setReturnValue(ChunkSectionLayer.TRANSLUCENT);
      }
   }

   @org.spongepowered.asm.mixin.Unique
   private static NoRenderModule schizo$noRenderModule() {
      return dev.schizo.client.bootstrap.MixinModuleRegistry.require(NoRenderModule.class);
   }

   @org.spongepowered.asm.mixin.Unique
   private static XrayModule schizo$xrayModule() {
      return dev.schizo.client.bootstrap.MixinModuleRegistry.require(XrayModule.class);
   }
}
