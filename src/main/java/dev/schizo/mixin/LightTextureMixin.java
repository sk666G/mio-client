/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.schizo.client.modules.render.AmbienceModule;
import dev.schizo.client.ClientContext;
import dev.schizo.client.render.ClientRenderPipelines;
import java.awt.Color;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(LightTexture.class)
public class LightTextureMixin {

   @Unique
   private static final Vector3f lightmapTint = new Vector3f();

   @ModifyExpressionValue(
      method = "updateLightTexture",
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderPipelines;LIGHTMAP:Lcom/mojang/blaze3d/pipeline/RenderPipeline;")
   )
   private RenderPipeline overrideLightmapPipeline(RenderPipeline originalPipeline) {
      if (schizo$ambienceModule().isToggled()
         && schizo$ambienceModule().brightnessSetting.getValue()
            == AmbienceModule.BrightnessChoice.SCREEN) {
         return ClientRenderPipelines.AMBIENCE;
      } else {
         return schizo$ambienceModule().isToggled()
               && schizo$ambienceModule().occlusionSetting.getValue()
               && schizo$ambienceModule().occlusionSetting
                  .isVisible()
            ? ClientRenderPipelines.OCCLUSION
            : originalPipeline;
      }
   }

   @ModifyArgs(
      method = "updateLightTexture",
      at = @At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putVec3(Lorg/joml/Vector3fc;)Lcom/mojang/blaze3d/buffers/Std140Builder;",
         ordinal = 1
      )
   )
   private void overrideLightmapTint(Args arguments) {
      boolean tintEnabled = schizo$ambienceModule().brightnessSetting.getValue()
            == AmbienceModule.BrightnessChoice.SCREEN
         || schizo$ambienceModule().occlusionSetting.getValue()
            && schizo$ambienceModule().occlusionSetting
               .isVisible();
      Color tintColor = schizo$ambienceModule().brightnessSetting.getValue()
            == AmbienceModule.BrightnessChoice.SCREEN
         ? schizo$ambienceModule().colorSetting.getValue()
         : schizo$ambienceModule().occlusionColorSetting.getValue();
      if (schizo$ambienceModule().isToggled() && tintEnabled) {
         arguments.set(
            0,
            lightmapTint.set(
                  tintColor.getRed(), tintColor.getGreen(), tintColor.getBlue()
               )
               .div(255.0F)
         );
      }
   }

   @ModifyReturnValue(method = "pack", at = @At("RETURN"))
   private static int overridePackedLight(int originalLight) {
      if (schizo$ambienceModule().isToggled()
         && schizo$ambienceModule().brightnessSetting.getValue()
            == AmbienceModule.BrightnessChoice.SCREEN) {
         return 0;
      } else {
         return schizo$ambienceModule().isToggled()
               && schizo$ambienceModule().occlusionSetting
                  .isVisible()
               && schizo$ambienceModule().occlusionSetting.getValue()
               && originalLight == 15728880
            ? 15728864
            : originalLight;
      }
   }

   @org.spongepowered.asm.mixin.Unique
   private static AmbienceModule schizo$ambienceModule() {
      return dev.schizo.client.bootstrap.MixinModuleRegistry.require(AmbienceModule.class);
   }
}
