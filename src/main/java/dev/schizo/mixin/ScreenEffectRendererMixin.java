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
import dev.schizo.client.modules.player.FreecamModule;
import dev.schizo.client.modules.render.NoRenderModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {


   @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
   private static void suppressFireOverlay(
      PoseStack poses, MultiBufferSource buffers, TextureAtlasSprite sprite, CallbackInfo callback
   ) {
      if (schizo$noRenderModule().isToggled()
         && schizo$noRenderModule().fireSetting.getValue()) {
         callback.cancel();
      }
   }

   @Inject(method = "renderWater", at = @At("HEAD"), cancellable = true)
   private static void suppressWaterOverlay(
      Minecraft client, PoseStack poses, MultiBufferSource buffers, CallbackInfo callback
   ) {
      if (schizo$noRenderModule().isToggled()
         && schizo$noRenderModule().blindnessSetting.getValue()) {
         callback.cancel();
      }
   }

   @Inject(method = "renderTex", at = @At("HEAD"), cancellable = true)
   private static void suppressTextureOverlay(
      TextureAtlasSprite sprite, PoseStack poses, MultiBufferSource buffers, CallbackInfo callback
   ) {
      if (schizo$noRenderModule().isToggled()
            && schizo$noRenderModule().blindnessSetting.getValue()
         || schizo$freecamModule().isToggled()) {
         callback.cancel();
      }
   }

   @org.spongepowered.asm.mixin.Unique
   private static NoRenderModule schizo$noRenderModule() {
      return dev.schizo.client.bootstrap.MixinModuleRegistry.require(NoRenderModule.class);
   }

   @org.spongepowered.asm.mixin.Unique
   private static FreecamModule schizo$freecamModule() {
      return dev.schizo.client.bootstrap.MixinModuleRegistry.require(FreecamModule.class);
   }
}
