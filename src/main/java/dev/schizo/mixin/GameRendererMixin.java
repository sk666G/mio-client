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
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.bootstrap.MixinModuleRegistry;
import dev.schizo.client.gui.screens.ClientScreen;
import dev.schizo.client.render.GuiDrawUtils;
import dev.schizo.client.utils.math.Vec3Utils;
import dev.schizo.client.managers.ShaderRegistry;
import dev.schizo.client.render.RenderUtils;
import dev.schizo.client.events.HudRenderEvent;
import dev.schizo.client.render.GuiProjectionUtils;
import dev.schizo.client.utils.LockUtils;
import dev.schizo.client.modules.render.FreeLookModule;
import dev.schizo.client.modules.player.FreecamModule;
import dev.schizo.client.modules.client.HUDModule;
import dev.schizo.client.modules.player.NameProtectModule;
import dev.schizo.client.modules.render.NoBobModule;
import dev.schizo.client.modules.render.NoRenderModule;
import dev.schizo.client.modules.render.ShaderModule;
import dev.schizo.client.modules.render.ViewModelModule;
import dev.schizo.client.modules.render.ZoomModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.fog.FogRenderer.FogMode;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
   @Unique
   private GuiGraphics hudGraphics;
   @Final
   @Shadow
   private Minecraft minecraft;
   @Shadow
   @Final
   public GuiRenderer guiRenderer;
   @Shadow
   @Final
   public FogRenderer fogRenderer;
   private boolean pickingFromFreecam = false;

   @Shadow
   public abstract void pick(float partialTick);

   @ModifyExpressionValue(method = "bobView", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;sin(D)F"))
   private float invertViewBobSine(float originalSine) {
      return RenderUtils.isInverseViewBobbing() ? -originalSine : originalSine;
   }

   @ModifyExpressionValue(method = "bobView", at = @At(value = "INVOKE", target = "Ljava/lang/Math;abs(F)F"))
   private float invertViewBobMagnitude(float originalMagnitude) {
      return RenderUtils.isInverseViewBobbing() ? -originalMagnitude : originalMagnitude;
   }

   @Inject(method = "<init>", at = @At("RETURN"))
   public void initializeShaderRegistry(
      Minecraft client, ItemInHandRenderer handRenderer, RenderBuffers renderBuffers, BlockRenderDispatcher blockRenderer, CallbackInfo callback
   ) {
      ClientContext.shaderRegistry = new ShaderRegistry();
   }

   @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
   private void suppressHurtCamera(
      PoseStack poses, float partialTick, CallbackInfo callback
   ) {
      if (MixinModuleRegistry.gameNoRender.isToggled() && MixinModuleRegistry.gameNoRender.hurtCamSetting.getValue()) {
         callback.cancel();
      }
   }

   @Inject(method = "displayItemActivation", at = @At("HEAD"), cancellable = true)
   private void suppressTotemActivation(ItemStack stack, CallbackInfo callback) {
      if (stack.getItem() == Items.TOTEM_OF_UNDYING && MixinModuleRegistry.gameNoRender.isToggled() && MixinModuleRegistry.gameNoRender.totemOverlaySetting.getValue()) {
         callback.cancel();
      }
   }

   @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(FFF)F"), require = 0)
   private float suppressBlindnessInterpolation(
      float fraction, float start, float end, Operation original
   ) {
      return MixinModuleRegistry.gameNoRender.isToggled() && MixinModuleRegistry.gameNoRender.blindnessSetting.getValue() ? 0.0F : Mth.lerp(fraction, start, end);
   }

   @Inject(
      method = "renderLevel",
      at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=level")
   )
   private void captureWorldProjectionMatrix(
      DeltaTracker deltaTracker,
      CallbackInfo callback,
      @Local(ordinal = 0) Matrix4f projectionMatrix,
      @Local(ordinal = 1) Matrix4f unusedSecondMatrix,
      @Local(ordinal = 1) float unusedRenderFloat,
      @Local PoseStack poses
   ) {
      RenderUtils.WORLD_PROJECTION_MATRIX.set(projectionMatrix);
   }

   @WrapWithCondition(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(FZLorg/joml/Matrix4f;)V"))
   private boolean shouldRenderHands(
      GameRenderer renderer, float partialTick, boolean ignoredHandFlag, Matrix4f projectionMatrix
   ) {
      return !MixinModuleRegistry.gameShader.isToggled() || !MixinModuleRegistry.gameShader.handsSetting.getValue();
   }

   @WrapWithCondition(method = "bobView", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
   private boolean applyNoBobTranslation(
      PoseStack poseStack, float x, float y, float z
   ) {
      if (!MixinModuleRegistry.gameNoBob.isToggled()) {
         return true;
      }

      float multiplier = MixinModuleRegistry.gameNoBob.multiplierSetting.getValue();
      if (multiplier == 0.0F) {
         return true;
      }

      poseStack.translate(x * multiplier, y * multiplier, z * multiplier);
      return false;
   }

   @ModifyReturnValue(method = "getFov", at = @At(value = "RETURN", ordinal = 1))
   private float applyZoomAndViewModelFov(
      float originalFov, @Local(argsOnly = true) boolean useFovSetting
   ) {
      if (useFovSetting) {
         return originalFov;
      }

      float zoomScale = 1.0F - MixinModuleRegistry.gameZoom.getZoomBlend();
      return MixinModuleRegistry.gameViewModel.isToggled() && MixinModuleRegistry.gameViewModel.viewModelFovSetting.getValue()
         ? (MixinModuleRegistry.gameViewModel.fovAmountSetting.getValue()).intValue() * zoomScale
         : originalFov * zoomScale;
   }

   @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
   private void prepareHandRendering(
      float partialTick, boolean ignoredHandFlag, Matrix4f projectionMatrix, CallbackInfo callback, @Share("backupMatrix") LocalRef backupMatrix
   ) {
      backupMatrix.set(new Matrix4f(projectionMatrix));
      RenderUtils.setRenderColor(
         1.0F,
         1.0F,
         1.0F,
         MixinModuleRegistry.gameNoRender.getHandOpacity()
      );
      if (MixinModuleRegistry.gameViewModel.isToggled() && MixinModuleRegistry.gameViewModel.lighterSetting.getValue()) {
         float handYaw = -95.0F;
         float handPitch = 45.0F;
         Quaternionf handRotation = new Quaternionf().rotationYXZ((float)Math.toRadians(180.0F - handYaw), (float)Math.toRadians(-handPitch), 0.0F).conjugate();
         projectionMatrix.set(new Matrix4f().rotation(handRotation));
      }

      if (MixinModuleRegistry.gameFreecam.shouldHideHands()) {
         callback.cancel();
      }
   }

   @Inject(method = "renderItemInHand", at = @At("RETURN"))
   private void restoreHandRendering(
      float partialTick, boolean ignoredHandFlag, Matrix4f projectionMatrix, CallbackInfo callback, @Share("backupMatrix") LocalRef backupMatrix
   ) {
      RenderUtils.setRenderColor(1.0F, 1.0F, 1.0F, 1.0F);
      if (MixinModuleRegistry.gameViewModel.isToggled() && MixinModuleRegistry.gameViewModel.lighterSetting.getValue()) {
         projectionMatrix.set((Matrix4fc)backupMatrix.get());
      }
   }

   @ModifyExpressionValue(method = "renderItemInHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;getPackedLightCoords(Lnet/minecraft/world/entity/Entity;F)I"))
   private int applyViewModelHandLight(int originalLight) {
      return MixinModuleRegistry.gameViewModel.isToggled() && MixinModuleRegistry.gameViewModel.lighterSetting.getValue()
         ? 15728864
         : originalLight;
   }

   @WrapWithCondition(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;renderScreenEffect(ZFLnet/minecraft/client/renderer/SubmitNodeCollector;)V"))
   private boolean shouldRenderScreenEffects(
      ScreenEffectRenderer renderer, boolean ignoredEffectFlag, float partialTick, SubmitNodeCollector collector
   ) {
      return !ClientContext.shaderRegistry.renderingIntoTarget;
   }

   @Inject(method = "pick", at = @At("HEAD"), cancellable = true)
   private void applyCameraPicking(float partialTick, CallbackInfo callback) {
      if (MixinModuleRegistry.gameFreecam.isToggled()
         && this.minecraft.getCameraEntity() != null
         && !this.pickingFromFreecam) {
         callback.cancel();
         Entity cameraEntity = this.minecraft.getCameraEntity();
         double savedX = cameraEntity.getX();
         double savedY = cameraEntity.getY();
         double savedZ = cameraEntity.getZ();
         double savedPreviousX = cameraEntity.xo;
         double savedPreviousY = cameraEntity.yo;
         double savedPreviousZ = cameraEntity.zo;
         float savedYaw = cameraEntity.getYRot();
         float savedPitch = cameraEntity.getXRot();
         float savedPreviousYaw = cameraEntity.yRotO;
         float savedPreviousPitch = cameraEntity.xRotO;
         cameraEntity.xo = MixinModuleRegistry.gameFreecam.previousCameraPosition.x;
         cameraEntity.yo = MixinModuleRegistry.gameFreecam.previousCameraPosition.y
            - cameraEntity.getEyeHeight(cameraEntity.getPose());
         cameraEntity.zo = MixinModuleRegistry.gameFreecam.previousCameraPosition.z;
         cameraEntity.setYRot(MixinModuleRegistry.gameFreecam.cameraYaw);
         cameraEntity.setYHeadRot(MixinModuleRegistry.gameFreecam.cameraYaw);
         cameraEntity.setXRot(MixinModuleRegistry.gameFreecam.cameraPitch);
         cameraEntity.yRotO = MixinModuleRegistry.gameFreecam.previousCameraYaw;
         cameraEntity.xRotO = MixinModuleRegistry.gameFreecam.previousCameraPitch;
         this.pickingFromFreecam = true;
         LockUtils.runWithSharedLock(
            () -> {
               Vec3Utils.setComponents(
                  cameraEntity.position(),
                  MixinModuleRegistry.gameFreecam.cameraPosition.x,
                  MixinModuleRegistry.gameFreecam.cameraPosition.y
                     - cameraEntity.getEyeHeight(cameraEntity.getPose()),
                  MixinModuleRegistry.gameFreecam.cameraPosition.z
               );
               this.pick(partialTick);
               Vec3Utils.setComponents(
                  cameraEntity.position(), savedX, savedY, savedZ
               );
            });
         this.pickingFromFreecam = false;
         cameraEntity.xo = savedPreviousX;
         cameraEntity.yo = savedPreviousY;
         cameraEntity.zo = savedPreviousZ;
         cameraEntity.setYRot(savedYaw);
         cameraEntity.setXRot(savedPitch);
         cameraEntity.yRotO = savedPreviousYaw;
         cameraEntity.xRotO = savedPreviousPitch;
      }

      if (MixinModuleRegistry.gameFreeLook.isToggled() && !MixinModuleRegistry.gameFreecam.isToggled()) {
         callback.cancel();
      }
   }

   @Inject(
      method = "render",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", shift = Shift.BEFORE)
   )
   private void beforeScreenRendering(
      DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo callback, @Local GuiGraphics graphics
   ) {
      NameProtectModule.replacementBypassed = true;
      boolean manageProjection;
      if (this.minecraft.screen instanceof ClientScreen clientScreen) {
         if (clientScreen.hasMenuParent()) {
            clientScreen.getParentScreen()
               .renderWithTooltipAndSubtitles(
                  graphics,
                  -99,
                  -99,
                  RenderUtils.getPartialTick()
               );
         }
         manageProjection = false;
      } else if (this.minecraft.screen != null) {
         manageProjection = true;
      } else {
         return;
      }
      this.guiRenderer.render(this.fogRenderer.getBuffer(FogMode.NONE));
      this.guiRenderer.incrementFrameNumber();
      RenderUtils.clearDepth(this.minecraft.getMainRenderTarget());
      if (!manageProjection) {
         GuiProjectionUtils.beginGuiProjection();
      }
      this.renderClientHud(graphics, manageProjection);
   }

   @Inject(
      method = "render",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", shift = Shift.AFTER)
   )
   private void afterScreenRendering(
      DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo callback
   ) {
      NameProtectModule.replacementBypassed = false;
      if (this.minecraft.screen instanceof ClientScreen) {
         GuiProjectionUtils.restoreProjection();
      }
   }

   @ModifyExpressionValue(
      method = "render",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getOverlay()Lnet/minecraft/client/gui/screens/Overlay;", ordinal = 0)
   )
   private Overlay captureHudGraphics(
      Overlay overlay, @Local GuiGraphics graphics
   ) {
      this.hudGraphics = graphics;
      PoseStack poses = GuiProjectionUtils.toPoseStack(graphics);
      if (ClientAccess.schizoMinecraft.level != null
         && !ClientAccess.schizoMinecraft.options.hideGui
         && !ClientAccess.schizoMinecraft.debugEntries.isOverlayVisible()) {
         GuiProjectionUtils.beginGuiProjection();
         HudRenderEvent.Before event = HudRenderEvent.Before.create(
            poses, graphics, RenderUtils.getPartialTick()
         );
         ClientAccess.eventBus.post(event);
         GuiDrawUtils.flush();
         GuiProjectionUtils.restoreProjection();
      }
      return overlay;
   }

   @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;incrementFrameNumber()V", shift = Shift.AFTER))
   private void renderHudWithoutScreen(
      DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo callback
   ) {
      if (this.minecraft.screen == null) {
         RenderUtils.clearDepth(this.minecraft.getMainRenderTarget());
         this.renderClientHud(
            this.hudGraphics, true
         );
      }
   }

   @Inject(method = "render", at = @At("HEAD"))
   private void updateFrameTimingAndChatHeight(
      DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo callback
   ) {
      MixinModuleRegistry.gameHud.updateChatInputHeight(
         ClientAccess.schizoMinecraft.screen instanceof ChatScreen
            ? 12.0F
            : 0.0F
      );
      ClientContext.frameTiming.recordFrame();
   }

   @Unique
   public void renderClientHud(GuiGraphics graphics, boolean manageProjection) {
      if (ClientAccess.schizoMinecraft.level == null) {
         return;
      }
      boolean nameReplacementWasBypassed = NameProtectModule.replacementBypassed;
      if (nameReplacementWasBypassed) {
         NameProtectModule.replacementBypassed = false;
      }

      if (manageProjection) {
         GuiProjectionUtils.beginGuiProjection();
      }

      PoseStack poses = GuiProjectionUtils.toPoseStack(graphics);
      if (!ClientAccess.schizoMinecraft.options.hideGui
         && !ClientAccess.schizoMinecraft.debugEntries.isOverlayVisible()) {
         HudRenderEvent hudEvent = HudRenderEvent.create(
            poses, graphics, RenderUtils.getPartialTick()
         );
         ClientAccess.eventBus
            .post(hudEvent);
      }

      HudRenderEvent.Unconditional unconditionalEvent = HudRenderEvent.Unconditional.create(
         poses, graphics, RenderUtils.getPartialTick()
      );
      ClientAccess.eventBus
         .post(unconditionalEvent);
      GuiDrawUtils.flush();
      if (manageProjection) {
         GuiProjectionUtils.restoreProjection();
      }

      if (nameReplacementWasBypassed) {
         NameProtectModule.replacementBypassed = true;
      }
   }
}
