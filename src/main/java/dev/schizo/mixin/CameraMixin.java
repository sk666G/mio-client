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
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import dev.schizo.client.events.CameraSetupEvent;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.bootstrap.MixinModuleRegistry;
import dev.schizo.client.utils.movement.EntityMotionAccess;
import dev.schizo.client.render.RenderUtils;
import dev.schizo.client.modules.movement.ElytraFlyModule;
import dev.schizo.client.modules.render.FreeLookModule;
import dev.schizo.client.modules.player.FreecamModule;
import dev.schizo.client.modules.render.NoRenderModule;
import dev.schizo.client.modules.render.ViewClipModule;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Camera.class)
public abstract class CameraMixin {
   @Final
   @Shadow
   private Vector3f forwards;
   @Final
   @Shadow
   private Vector3f up;
   @Final
   @Shadow
   private Vector3f left;
   @Shadow
   private Vec3 position;
   @Shadow
   private float eyeHeight;
   @Shadow
   private float eyeHeightOld;
   @Shadow
   private Entity entity;
   @Shadow
   @Final
   private EnvironmentAttributeProbe attributeProbe;
   @Shadow
   private Level level;

   @Shadow
   protected void setPosition(Vec3 position) {
   }

   @Shadow
   protected abstract void setRotation(float yaw, float pitch);

   @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
   private void applyViewClipAndFreeLookDistance(
      float originalDistance, CallbackInfoReturnable<Float> callback, @Local(argsOnly = true) LocalFloatRef distanceRef
   ) {
      if (MixinModuleRegistry.levelViewClip.isToggled()) {
         float cameraDistance = MixinModuleRegistry.levelViewClip.getCameraDistance();
         if (MixinModuleRegistry.gameFreeLook.isScrollEnabled()) {
            cameraDistance += MixinModuleRegistry.gameFreeLook.getCameraDistanceOffset();
            cameraDistance = Mth.clamp(cameraDistance, 1.0F, 20.0F);
         }

         callback.setReturnValue(cameraDistance);
         return;
      }
      if (MixinModuleRegistry.gameFreeLook.isScrollEnabled()) {
         distanceRef.set(
            Mth.clamp(
               distanceRef.get()
                  + MixinModuleRegistry.gameFreeLook.getCameraDistanceOffset(),
               1.0F,
               20.0F
            )
         );
      }
   }

   @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
   private void applyLegacySneakingCamera(CallbackInfo callback) {
      if (!MixinModuleRegistry.levelNoRender.isToggled() || !MixinModuleRegistry.levelNoRender.newSneakingSetting.getValue() || this.entity == null) {
         return;
      }
      callback.cancel();
      this.eyeHeightOld = this.eyeHeight;
      if (this.entity instanceof Player cameraPlayer) {
         if (cameraPlayer.hasPose(Pose.CROUCHING)) {
            this.eyeHeight = 1.54F;
         } else if (this.eyeHeight < 1.62F && cameraPlayer.hasPose(Pose.STANDING)) {
            float heightDifference = 1.62F - this.eyeHeight;
            heightDifference *= 0.4F;
            this.eyeHeight = 1.62F - heightDifference;
         } else {
            this.eyeHeight = this.eyeHeight + (this.entity.getEyeHeight() - this.eyeHeight) * 0.5F;
         }
      }

      this.attributeProbe.tick(this.level, this.position);
   }

   @Inject(method = "move", at = @At("HEAD"), cancellable = true)
   private void applySmoothViewClipMove(float forward, float vertical, float sideways, CallbackInfo callback) {
      if (MixinModuleRegistry.levelViewClip.isToggled() && MixinModuleRegistry.levelViewClip.smoothSetting.getValue()) {
         double offsetX = (double)this.forwards.x() * forward + (double)this.up.x() * vertical + (double)this.left.x() * sideways;
         double offsetY = (double)this.forwards.y() * forward + (double)this.up.y() * vertical + (double)this.left.y() * sideways;
         double offsetZ = (double)this.forwards.z() * forward + (double)this.up.z() * vertical + (double)this.left.z() * sideways;
         float blend = MixinModuleRegistry.levelViewClip.clipAnimation.updateAndGet();
         this.setPosition(new Vec3(this.position.x + offsetX * blend, this.position.y + offsetY * blend, this.position.z + offsetZ * blend));
         callback.cancel();
      }
   }

   @ModifyArgs(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"))
   private void modifySetupPosition(
      Args args, @Local(argsOnly = true) Entity entity, @Local(argsOnly = true) float partialTick
   ) {
      CameraSetupEvent event = new CameraSetupEvent(
         entity,
         partialTick,
         args.<Double>get(0),
         args.<Double>get(1),
         args.<Double>get(2)
      );
      ClientAccess.eventBus
         .post(event);
      if (event.isCancelled()) {
         args.set(0, event.getPosition().x);
         args.set(1, event.getPosition().y);
         args.set(
            2,
            event.getPosition().z
         );
      }
   }

   @ModifyArgs(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"))
   private void applyCameraSetupRotations(Args arguments) {
      if (MixinModuleRegistry.entityElytraFlyModule.isToggled()
         && MixinModuleRegistry.entityElytraFlyModule.modeSetting.getValue() == ElytraFlyModule.ModeChoice.BOUNCE
         && MixinModuleRegistry.entityElytraFlyModule.pitchLockSetting.getValue()
         && !MixinModuleRegistry.entityElytraFlyModule.silentSetting.getValue()) {
         arguments.set(1, MixinModuleRegistry.entityElytraFlyModule.pitchSetting.getValue());
      }

      float partialTick = RenderUtils.getPartialTick();
      if (MixinModuleRegistry.levelFreecam.isToggled()) {
         arguments.set(
            0,
            (float)MixinModuleRegistry.levelFreecam.getInterpolatedYaw(partialTick)
         );
         arguments.set(
            1,
            (float)MixinModuleRegistry.levelFreecam.getInterpolatedPitch(partialTick)
         );
      }
   }

   @Inject(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 1, shift = Shift.AFTER))
   public void applyFreeLookRotation(
      Level level, Entity cameraEntity, boolean detached, boolean mirrored, float partialTick, CallbackInfo callback
   ) {
      if (!MixinModuleRegistry.levelFreecam.isToggled() && MixinModuleRegistry.gameFreeLook.isToggled() && cameraEntity instanceof EntityMotionAccess motionAccess) {
         this.setRotation(motionAccess.getCameraYaw(), motionAccess.getCameraPitch());
      }
   }
}
