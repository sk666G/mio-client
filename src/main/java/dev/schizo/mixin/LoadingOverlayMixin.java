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
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.schizo.client.ClientContext;
import dev.schizo.client.bootstrap.MixinModuleRegistry;
import dev.schizo.client.audio.BuiltinSound;
import dev.schizo.client.render.LoadingOverlayRenderer;
import dev.schizo.client.render.LoadingMotion;
import dev.schizo.client.render.LoadingOverlayTheme;
import dev.schizo.client.modules.client.SettingsModule;
import dev.schizo.client.utils.collections.NamedIdentifier;
import java.awt.Color;
import java.util.Calendar;
import java.util.Date;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin extends Overlay {
   @Shadow
   private long fadeOutStart;
   @Shadow
   @Final
   private Minecraft minecraft;
   @Unique
   private LoadingOverlayTheme loadingTheme;
   @Unique
   private static boolean startupSoundPlayed;
   @Unique
   private static boolean loadingRenderStarted;
   @Shadow
   private float currentProgress;
   @Shadow
   private long fadeInStart;

   @Inject(method = "<init>", at = @At("TAIL"))
   private void initializeLoadingTheme(
      Minecraft client, ReloadInstance reload, Consumer onFinish, boolean fadeIn, CallbackInfo callback
   ) {
      // Keep the Schizo terminal splash consistent on every world transition.
      this.loadingTheme = LoadingOverlayTheme.DEFAULT;
   }

   @Shadow
   private static int replaceAlpha(int color, int alpha) {
      return 0;
   }

   @Inject(method = "render", at = @At("HEAD"))
   private void handleStartupEffects(
      GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback
   ) {
      if (!startupSoundPlayed
         && loadingRenderStarted) {
         if (Boolean.getBoolean("schizo.maximizedWindow")) {
            long windowHandle = this.minecraft.getWindow().handle();
            if (GLFW.glfwGetWindowMonitor(windowHandle) == 0L) {
               GLFW.glfwMaximizeWindow(windowHandle);
            }
         }

         // Mixins may be initialized before Schizo's module registry and sound
         // manager. Resolve these lazily so world loads/disconnects cannot NPE.
         SettingsModule settingsModule = MixinModuleRegistry.getModule(SettingsModule.class);
         if (settingsModule != null && ClientContext.soundManager != null) {
            NamedIdentifier startupSound = settingsModule.getStartupSound();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            if (calendar.get(Calendar.MONTH) == Calendar.OCTOBER
               && calendar.get(Calendar.DAY_OF_MONTH) >= 26) {
               startupSound = BuiltinSound.HORROR.getIdentifier();
            }
            if (startupSound != null) {
               ClientContext.soundManager.getSound(startupSound).play(0.5F);
            }
         }
         startupSoundPlayed = true;
      }

      loadingRenderStarted = true;
   }

   @WrapWithCondition(
      method = "render",
      at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearColorTexture(Lcom/mojang/blaze3d/textures/GpuTexture;I)V")
   )
   private boolean replaceDefaultThemeClear(
      CommandEncoder encoder, GpuTexture texture, int originalColor
   ) {
      if (this.loadingTheme == LoadingOverlayTheme.DEFAULT) {
         encoder.clearColorTexture(
            texture,
            1644825
         );
         return false;
      } else {
         return true;
      }
   }

   @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
   private boolean allowVanillaBackgroundFill(
      GuiGraphics graphics, int left, int top, int right, int bottom, int color
   ) {
      return this.loadingTheme != LoadingOverlayTheme.DEFAULT;
   }

   @WrapWithCondition(
      method = "render",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIIII)V"
      )
   )
   private boolean allowVanillaLogoBlit(
      GuiGraphics graphics,
      RenderPipeline pipeline,
      Identifier texture,
      int x,
      int y,
      float u,
      float v,
      int width,
      int height,
      int regionWidth,
      int regionHeight,
      int textureWidth,
      int textureHeight,
      int color
   ) {
      return this.loadingTheme != LoadingOverlayTheme.DEFAULT;
   }

   @Inject(
      method = "render",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIIII)V",
         ordinal = 0,
         shift = Shift.BEFORE
      )
   )
   private void renderLoadingThemeBackground(
      GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback
   ) {
      int screenWidth = this.minecraft.getWindow().getGuiScaledWidth();
      int screenHeight = this.minecraft.getWindow().getGuiScaledHeight();
      long now = Util.getMillis();
      float fade = LoadingMotion.opacity(now, this.fadeInStart, this.fadeOutStart);
      int opacityByte = Math.round(fade * 255.0F);

      if (this.loadingTheme == LoadingOverlayTheme.DEFAULT) {
         LoadingOverlayRenderer.render(graphics, fade, 1.0F,
            LoadingMotion.logoScale(fade, dev.schizo.client.gui.UiMotionPolicy.isReducedMotion()));
      } else {
         for (int stripeIndex = 0;
            stripeIndex
               < this.loadingTheme
                  .getColors()
                  .size();
            stripeIndex++
         ) {
            graphics.fill(
               0,
               stripeIndex
                  * (
                     screenHeight
                        / this.loadingTheme
                           .getColors()
                           .size()
                  ),
               screenWidth,
               (stripeIndex + 1)
                  * (
                     screenHeight
                        / this.loadingTheme
                           .getColors()
                           .size()
                  ),
               replaceAlpha(
                  ((Color)this.loadingTheme
                        .getColors()
                        .get(stripeIndex))
                     .hashCode(),
                  opacityByte
               )
            );
         }
      }
   }

   @Inject(method = "drawProgressBar", at = @At("HEAD"), cancellable = true)
   private void renderDefaultThemeProgressBar(
      GuiGraphics graphics, int left, int top, int right, int bottom, float opacity, CallbackInfo callback
   ) {
      if (this.loadingTheme == LoadingOverlayTheme.DEFAULT) {
         int width = graphics.guiWidth(), height = graphics.guiHeight();
         if (width > 0 && height > 0) {
            int railWidth = Math.min(240, Math.max(1, width - 24));
            int railLeft = (width - railWidth) / 2;
            int railY = Math.max(0, height - 17);
            float alpha = LoadingMotion.unit(opacity);
            int track = replaceAlpha(0xff33343e, Math.round(alpha * 255));
            int fill = replaceAlpha(0xffe7394d, Math.round(alpha * 255));
            int progressWidth = LoadingMotion.progressWidth(railWidth, this.currentProgress);
            graphics.fill(railLeft, railY, railLeft + railWidth, Math.min(height, railY + 3), track);
            if (progressWidth > 0) graphics.fill(railLeft, railY, railLeft + progressWidth, Math.min(height, railY + 3), fill);
         }
         callback.cancel();
      }
   }
}
