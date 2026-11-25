/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.schizo.client.ClientContext;
import dev.schizo.client.render.ClientRenderPipelines;
import dev.schizo.client.modules.render.TooltipsModule;
import java.awt.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TooltipRenderUtil.class)
public class TooltipRenderUtilMixin {

   @WrapWithCondition(
      method = "renderTooltipBackground",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
         ordinal = 1
      )
   )
   private static boolean renderColoredTooltipSprite(
      GuiGraphics graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height
   ) {
      Color tooltipColor = schizo$tooltipsModule().getTooltipColor();
      if (schizo$tooltipsModule().isToggled() && tooltipColor != null) {
         graphics.blitSprite(
            ClientRenderPipelines.POSITION_TEX_COLOR,
            sprite,
            x,
            y,
            width,
            height,
            tooltipColor.hashCode()
         );
         return false;
      } else {
         return true;
      }
   }

   @org.spongepowered.asm.mixin.Unique
   private static TooltipsModule schizo$tooltipsModule() {
      return dev.schizo.client.bootstrap.MixinModuleRegistry.require(TooltipsModule.class);
   }
}
