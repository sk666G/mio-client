/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBuffer.MappedView;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.schizo.client.bootstrap.MixinModuleRegistry;
import dev.schizo.client.modules.render.SkyboxModule;
import java.awt.Color;
import java.nio.ByteBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
   @Unique
   private SkyboxModule.FogDistances capturedFogDistances;

   @Inject(method = "updateBuffer", at = @At("HEAD"))
   private void captureFogDistances(
      ByteBuffer buffer, int bufferOffset, Vector4f fogColor, float environmentalStart, float environmentalEnd, float renderDistanceStart, float renderDistanceEnd, float skyEnd, float cloudsEnd, CallbackInfo callback
   ) {
      this.capturedFogDistances = new SkyboxModule.FogDistances(
         environmentalStart, environmentalEnd, renderDistanceStart, renderDistanceEnd, skyEnd, cloudsEnd
      );
   }

   @Inject(
      method = "setupFog",
      at = @At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/systems/RenderSystem;getDevice()Lcom/mojang/blaze3d/systems/GpuDevice;",
         shift = Shift.BEFORE,
         remap = false
      )
   )
   private void applyNoRenderFogSettings(
      Camera camera, int renderDistance, DeltaTracker deltaTracker, float ignoredSetupFloat, ClientLevel level, CallbackInfoReturnable<Vector4f> callback, @Local Vector4f fogColor, @Local FogData fogData
   ) {
      if (MixinModuleRegistry.fogNoRender.isToggled()
         && MixinModuleRegistry.fogNoRender.fogSetting.getValue()) {
         MixinModuleRegistry.fogNoRender.applyFogSettings(
            fogData, renderDistance
         );
      }

      if (MixinModuleRegistry.fogSkybox.isToggled()
         && MixinModuleRegistry.fogSkybox.isCurrentDimensionEnabled()) {
         Color configuredColor = MixinModuleRegistry.fogSkybox.fogSetting.getValue();
         float red = configuredColor.getRed() / 255.0F;
         float green = configuredColor.getGreen() / 255.0F;
         float blue = configuredColor.getBlue() / 255.0F;
         float alpha = configuredColor.getAlpha() / 255.0F;
         fogColor.set(red, green, blue, alpha);
      }
   }

   @Inject(method = "setupFog", at = @At("TAIL"))
   private void updateSplitSkyFogBuffer(
      Camera camera, int renderDistance, DeltaTracker deltaTracker, float ignoredSetupFloat, ClientLevel level, CallbackInfoReturnable<Vector4f> callback
   ) {
      if (MixinModuleRegistry.fogSkybox.isToggled()
         && MixinModuleRegistry.fogSkybox.splitFogSetting.getValue()
         && this.capturedFogDistances != null) {
         Color backgroundColor = MixinModuleRegistry.fogSkybox.backgroundSetting.getValue();
         Vector4f skyColor = new Vector4f(backgroundColor.getRed(), backgroundColor.getGreen(), backgroundColor.getBlue(), backgroundColor.getAlpha()).div(255.0F);

         try (MappedView mappedBuffer = RenderSystem.getDevice()
               .createCommandEncoder()
               .mapBuffer(
                  MixinModuleRegistry.fogSkybox.getSkyFogBuffer()
                     .currentBuffer(),
                  false,
                  true
               )) {
            this.updateBuffer(
               mappedBuffer.data(),
               0,
               skyColor,
               this.capturedFogDistances
                  .environmentalStart(),
               this.capturedFogDistances
                  .environmentalEnd(),
               this.capturedFogDistances
                  .renderDistanceStart(),
               this.capturedFogDistances
                  .renderDistanceEnd(),
               this.capturedFogDistances
                  .skyEnd(),
               this.capturedFogDistances
                  .cloudsEnd()
            );
         }
      }
   }

   @Shadow
   protected abstract void updateBuffer(ByteBuffer buffer, int bufferOffset, Vector4f fogColor, float environmentalStart, float environmentalEnd, float renderDistanceStart, float renderDistanceEnd, float skyEnd, float cloudsEnd);
}
