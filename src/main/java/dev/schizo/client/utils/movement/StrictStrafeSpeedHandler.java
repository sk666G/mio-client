/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils.movement;

import dev.schizo.client.utils.entity.PlayerStateUtils;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.utils.math.MathConstants;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.LocalPlayerMoveEvent;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.movement.SpeedModeHandler;
import dev.schizo.client.modules.movement.LongJumpModule;
import dev.schizo.client.modules.movement.SpeedModule;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class StrictStrafeSpeedHandler extends SpeedModeHandler {
   private static LongJumpModule longJumpModule;

   @Override
   public void onRotationUpdate() {
      if (this.shouldPauseStrictStrafe() || !MovementPhysics.isLocalPlayerMoving()
         || !ClientAccess.schizoMinecraft.player.verticalCollisionBelow) {
         return;
      }
      LocalPlayer player = ClientAccess.schizoMinecraft.player;
      double[] motion = MovementPhysics.getHorizontalMotion(player.getYRot(), player.input, 1.0D);
      float movementYaw = (float)(Math.toDegrees(Math.atan2(motion[1], motion[0])) - MathConstants.NINETY_DEGREES);
      float radians = (float)Math.toRadians((double)movementYaw);
      Vec3 velocity = player.getDeltaMovement();
      player.setDeltaMovement(velocity.x, 0.4D + MovementPhysics.getJumpBoost(), velocity.z);
      player.addDeltaMovement(new Vec3(-Mth.sin((double)radians) * 0.2F, 0.0, Mth.cos((double)radians) * 0.2F));
      player.setSprinting(true);
   }

   public StrictStrafeSpeedHandler(SpeedModule speedModule) {
      super(speedModule);
   }

   @Override
   public void onMove(LocalPlayerMoveEvent event) {
      if (this.shouldPauseStrictStrafe()) {
         return;
      }
      if (this.speedModule.useTimerSetting.getValue()
         && ClientContext.serverStateManager.getSetbackTimer().hasElapsed(250L)) {
         ClientContext.tickRateController.requestMultiplier(this.speedModule, 1.0888F);
      } else {
         ClientContext.tickRateController.releaseMultiplier(this.speedModule);
      }
      if (!MovementPhysics.isLocalPlayerMoving()) {
         event.setHorizontalVelocity(0.0, 0.0);
         return;
      }
      double speed = MovementPhysics.getBaseMoveSpeed(false);
      if (ClientContext.serverStateManager.getSetbackTimer().hasElapsed(500L)) {
         speed = Math.max(speed, Math.hypot(event.getX(), event.getZ()));
      }
      MovementPhysics.applyHorizontalMotion(event, speed);
   }

   private boolean shouldPauseStrictStrafe() {
      return this.speedModule.shouldPauseInBlocks() || this.speedModule.shouldPauseInLiquid()
         || PlayerStateUtils.isHungerLimited() || ClientAccess.schizoMinecraft.player.isFallFlying()
         || ClientAccess.schizoMinecraft.player.isSpectator() || longJumpModule.isToggled();
   }

   public static void bindModules(ModuleRegistry modules) {
      longJumpModule = (LongJumpModule)modules.getModule(
         LongJumpModule.class
      );
   }

}
