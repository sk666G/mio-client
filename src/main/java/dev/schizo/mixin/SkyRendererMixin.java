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
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.schizo.client.ClientContext;
import dev.schizo.client.render.RenderTargetUtils;
import dev.schizo.client.render.MaskShaderRenderer;
import dev.schizo.client.render.RenderUtils;
import dev.schizo.client.modules.render.NoRenderModule;
import dev.schizo.client.modules.render.SkyboxModule;
import java.awt.Color;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.SkyRenderState;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.dimension.DimensionType.Skybox;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(SkyRenderer.class)
public class SkyRendererMixin {


   @Unique
   private static boolean hook = false;

   @ModifyExpressionValue(
      method = "extractRenderState",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/dimension/DimensionType;skybox()Lnet/minecraft/world/level/dimension/DimensionType$Skybox;")
   )
   private Skybox overrideDimensionSkybox(Skybox originalSkybox) {
      if (schizo$skyboxModule().isToggled()
         && schizo$skyboxModule().isCurrentDimensionEnabled()) {
         return switch (schizo$skyboxModule().typeSetting.getValue()) {
            case NONE -> originalSkybox;
            case OVERWORLD -> Skybox.OVERWORLD;
            case FLAT -> Skybox.NONE;
            case END -> Skybox.END;
            default -> throw new MatchException(null, null);
         };
      } else {
         return originalSkybox;
      }
   }

   @Inject(method = "extractRenderState", at = @At("TAIL"))
   private void overrideSkyRenderState(
      ClientLevel level, float partialTick, Camera camera, SkyRenderState renderState, CallbackInfo callback
   ) {
      if (schizo$skyboxModule().isToggled()
         && schizo$skyboxModule().isCurrentDimensionEnabled()) {
         renderState.skyColor = (schizo$skyboxModule().skySetting.getValue())
            .hashCode();
         if (schizo$skyboxModule().typeSetting.getValue()
            == SkyboxModule.SkyType.FLAT) {
            renderState.shouldRenderDarkDisc = false;
         }
      }
   }

   @Inject(method = "renderSun", at = @At("HEAD"), cancellable = true)
   private void hideSun(float brightness, PoseStack poses, CallbackInfo callback) {
      if (schizo$noRenderModule().shouldHideCelestialBodies()) {
         callback.cancel();
      }
   }

   @Inject(method = "renderMoon", at = @At("HEAD"), cancellable = true)
   private void hideMoon(MoonPhase phase, float brightness, PoseStack poses, CallbackInfo callback) {
      if (schizo$noRenderModule().shouldHideCelestialBodies()) {
         callback.cancel();
      }
   }

   @Inject(method = "renderStars", at = @At("HEAD"), cancellable = true)
   private void hideStars(float brightness, PoseStack poses, CallbackInfo callback) {
      if (schizo$noRenderModule().shouldHideStars()) {
         callback.cancel();
      }
   }

   @Inject(method = "renderStars", at = @At("TAIL"))
   private void renderFancyStarBloom(
      float brightness, PoseStack poses, CallbackInfo callback
   ) {
      if (schizo$skyboxModule().isToggled()
         && schizo$skyboxModule().fancyStarsSetting.getValue()) {
         if (!hook) {
            this.renderStarsToBloomMask(brightness, poses);
         }
      }
   }

   @ModifyArgs(
      method = "renderStars",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/DynamicUniforms;writeTransform(Lorg/joml/Matrix4fc;Lorg/joml/Vector4fc;Lorg/joml/Vector3fc;Lorg/joml/Matrix4fc;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"
      )
   )
   public void overrideStarTint(Args arguments) {
      if (schizo$skyboxModule().isToggled()
         && schizo$skyboxModule().fancyStarsSetting.getValue()) {
         Color starColor = schizo$skyboxModule().starsColorSetting.getValue();
         float originalAlpha = arguments.<Vector4f>get(1).w();
         arguments.set(1, new Vector4f(starColor.getRed() / 255.0F, starColor.getGreen() / 255.0F, starColor.getBlue() / 255.0F, originalAlpha));
      }
   }

   @Unique
   private void renderStarsToBloomMask(float brightness, PoseStack poses) {
      RenderTarget previousTarget = Minecraft.getInstance().getMainRenderTarget();
      RenderUtils.clearColor(
         MaskShaderRenderer.MASK_TARGET,
         schizo$skyboxModule().starsColorSetting.getValue()
      );
      RenderUtils.clearDepth(
         MaskShaderRenderer.MASK_TARGET
      );
      RenderTargetUtils.setMainRenderTarget(
         MaskShaderRenderer.MASK_TARGET
      );
      hook = true;
      this.renderStars(brightness, poses);
      hook = false;
      RenderTargetUtils.setMainRenderTarget(previousTarget);
      MaskShaderRenderer.setParameters(4.0F, 1.5F, 1.0F, 2.0F);
      MaskShaderRenderer.renderEffect("bloom_mask");
   }

   @Shadow
   private void renderStars(float brightness, PoseStack poses) {
   }

   @org.spongepowered.asm.mixin.Unique
   private static SkyboxModule schizo$skyboxModule() {
      return dev.schizo.client.bootstrap.MixinModuleRegistry.require(SkyboxModule.class);
   }

   @org.spongepowered.asm.mixin.Unique
   private static NoRenderModule schizo$noRenderModule() {
      return dev.schizo.client.bootstrap.MixinModuleRegistry.require(NoRenderModule.class);
   }
}
