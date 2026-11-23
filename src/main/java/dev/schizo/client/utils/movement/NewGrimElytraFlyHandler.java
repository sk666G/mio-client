/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils.movement;

import dev.schizo.client.modules.movement.ElytraFlyModule.StopMotionChoice;

import dev.schizo.client.managers.RotationManager;
import dev.schizo.client.managers.SocialRegistry;
import dev.schizo.client.managers.TickRateController;
import dev.schizo.client.utils.entity.EntityUtils;
import dev.schizo.client.utils.entity.PlayerStateUtils;
import dev.schizo.client.utils.math.Vec3Utils;
import dev.schizo.client.utils.network.PacketSender;

import static dev.schizo.client.utils.math.MathConstants.NINETY_DEGREES;

import static dev.schizo.client.utils.math.MathConstants.FORTY_FIVE_DEGREES;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.events.RotationRequestEvent;
import dev.schizo.client.events.FireworkRocketEntityTickEvent;
import dev.schizo.client.events.LocalPlayerMoveEvent;
import dev.schizo.client.events.MinecraftRunTickEvent;
import dev.schizo.client.managers.ModuleRegistry;
import dev.schizo.client.modules.client.AntiCheatModule;
import dev.schizo.client.modules.combat.AutoMaceModule;
import dev.schizo.client.modules.movement.ElytraFlyModule;
import dev.schizo.client.settings.Setting;

import java.util.Iterator;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;

public class NewGrimElytraFlyHandler extends ElytraFlyModeHandler {
   private static AutoMaceModule autoMaceModule;
   private boolean movementStarted;
   private boolean negativeYawJitter;
   private boolean pitchJitterPhase;
   private float serverYaw;
   private float serverPitch;

   public NewGrimElytraFlyHandler(ElytraFlyModule module) {
      super(module);
   }

   @Override
   public void handleFireworkTick(FireworkRocketEntityTickEvent event) {
      if (autoMaceModule.isToggled()) {
         if (autoMaceModule.isFlightControlActive()) {
            return;
         }
      }

      float yaw = this.serverYaw;
      event.setYaw(yaw);
      float pitch = this.serverPitch;
      event.setPitch(pitch);
   }

   @Override
   public void handleMovement(LocalPlayerMoveEvent event) {
      if (EntityUtils.hasCollisionBelow(ClientAccess.schizoMinecraft.player)) {
         this.movementStarted = false;
      }

      if (this.isIdleAirborne()) {
         if (this.hasDurabilityFlightCondition()) {
            Setting<StopMotionChoice> stopMotionSetting = this.module.stopMotionSetting;
            if (stopMotionSetting.getValue() == ElytraFlyModule.StopMotionChoice.STATIC) {
               if (autoMaceModule.isToggled()) {
                  if (autoMaceModule.isFlightControlActive()) {
                     return;
                  }
               }

               Vec3 stoppedVelocity = Vec3.ZERO;
               event.setVelocity(stoppedVelocity);
            }
         }
      }
   }

   @Override
   public void handleClientTick(MinecraftRunTickEvent event) {
      ElytraFlyModule moduleForTimerRelease = this.module;
      TickRateController timerControllerForRelease = ClientContext.tickRateController;
      timerControllerForRelease.releaseMultiplier(moduleForTimerRelease);
      if (autoMaceModule.isToggled()) {
         if (autoMaceModule.isFlightControlActive()) {
            return;
         }
      }

      if (!this.isWorldUnavailable()) {
         Setting<StopMotionChoice> stopMotionSetting = this.module.stopMotionSetting;
         if (stopMotionSetting.getValue() == ElytraFlyModule.StopMotionChoice.TIMER) {
            if (this.hasDurabilityFlightCondition()) {
               this.movementStarted = true;

               if (this.isIdleAirborne()) {
                  TickRateController timerControllerForRequest = ClientContext.tickRateController;
                  ElytraFlyModule moduleForTimerRequest = this.module;

                  timerControllerForRequest.requestMultiplier(moduleForTimerRequest, 0.02F);
               }

               return;
            }
         }
      }
   }

   @Override
   public void handleRotationManagerEvent(RotationRequestEvent event) {
      if (!EntityUtils.hasCollisionBelow(ClientAccess.schizoMinecraft.player)) {
         if (autoMaceModule.isToggled()) {
            if (autoMaceModule.isFlightControlActive()) {
               return;
            }
         }
         if (PlayerStateUtils.isLocalWearingElytra()) {
            ElytraFlyModule.InfDurabilityChoice durabilityChoice = this.module.infDurabilitySetting.getValue();
            if (durabilityChoice.isDisabled()) {
               if (!PlayerStateUtils.isFallFlying()) {
                  ClientAccess.schizoMinecraft.player.startFallFlying();

                  PacketSender.sendStartFallFlying();
               }
            }
         }
         if (this.hasDurabilityFlightCondition()) {
            float playerYaw = ClientAccess.schizoMinecraft.player.getYRot();
            ClientInput movementInput = ClientAccess.schizoMinecraft.player.input;
            double[] horizontalMotion = MovementPhysics.getHorizontalMotion(
               playerYaw, movementInput, 1.0D
            );
            double directionZ = horizontalMotion[1];
            double directionX = horizontalMotion[0];
            double directionRadians = Math.atan2(directionZ, directionX);
            float yaw = (float)(Math.toDegrees(directionRadians)
               - NINETY_DEGREES);

            if (!MovementPhysics.isLocalPlayerMoving()) {
               yaw = playerYaw;
            }
            yaw += (this.negativeYawJitter ? -1 : 1) * AntiCheatModule.ROTATION_JITTER_STEP;

            this.negativeYawJitter = !this.negativeYawJitter;

            float pitch = -1.0F;
            if (ClientAccess.schizoMinecraft.player.input.keyPresses.jump()) {
               pitch = MovementPhysics.isLocalPlayerMoving()
                  ? -FORTY_FIVE_DEGREES
                  : -NINETY_DEGREES;

               if (!MovementPhysics.isLocalPlayerMoving()) {
                  float stationaryJumpYaw = yaw + 180.0F;
                  yaw = Mth.wrapDegrees(stationaryJumpYaw);
               }
            }

            if (ClientAccess.schizoMinecraft.player.input.keyPresses.shift()) {
               Boolean responsiveDescend = this.module.responsiveDescendSetting.getValue();
               if (responsiveDescend) {
                  float responsiveDescendPitch;
                  if (MovementPhysics.isLocalPlayerMoving()) {
                     responsiveDescendPitch = ClientAccess.schizoMinecraft.player.getXRot();
                  } else {
                     responsiveDescendPitch = NINETY_DEGREES;
                  }

                  pitch = responsiveDescendPitch;
               } else {
                  pitch = MovementPhysics.isLocalPlayerMoving()
                     ? FORTY_FIVE_DEGREES
                     : NINETY_DEGREES;
               }
            }

            Setting<StopMotionChoice> stopMotionSetting = this.module.stopMotionSetting;
            if (stopMotionSetting.getValue() == ElytraFlyModule.StopMotionChoice.MOTION) {
               if (this.isIdleAirborne()) {
                  if (this.pitchJitterPhase) {
                     float jitteredYaw = yaw + 180.0F;
                     yaw = Mth.wrapDegrees(jitteredYaw);
                  }

                  this.pitchJitterPhase = !this.pitchJitterPhase;
               }
            }

            this.serverYaw = yaw;
            this.serverPitch = pitch;
            RotationManager rotationManagerForRequest = ClientContext.rotationManager;

            float[] rotation = {yaw, pitch};

            rotationManagerForRequest.requestRotation(rotation, 999999, true);
            ClientContext.rotationManager.forceRotationOverride();
         }
      }
   }

   private boolean isIdleAirborne() {
      Input movementInput = ClientAccess.schizoMinecraft.player.input.keyPresses;
      boolean inputRequested = MovementPhysics.isLocalPlayerMoving()
         || movementInput.jump() || movementInput.shift() || this.isMovementKeyDown();
      return !EntityUtils.hasCollisionBelow(ClientAccess.schizoMinecraft.player) && !inputRequested;
   }

   public boolean shouldFallWithChestplate() {
      if (!this.module.isToggled() || !this.movementStarted) {
         return false;
      }
      if (!ClientAccess.schizoMinecraft.player.input.keyPresses.shift()) {
         return false;
      }
      Boolean fallChestplateEnabled = this.module.fallChestplateSetting.getValue();
      if (!fallChestplateEnabled) {
         return false;
      }
      float nearestPlayerDistance = 999.0F;
      ClientLevel level = ClientAccess.schizoMinecraft.level;
      Iterator<AbstractClientPlayer> players = level.players().iterator();
      while (players.hasNext()) {
         AbstractClientPlayer otherPlayer = players.next();
         if (otherPlayer == ClientAccess.schizoMinecraft.player) {
            continue;
         }
         SocialRegistry socialRegistry = ClientContext.socialRegistry;
         if (socialRegistry.isFriendPlayer(otherPlayer)) {
            continue;
         }
         double otherPlayerY = otherPlayer.getY();
         LocalPlayer playerForHeightCheck = ClientAccess.schizoMinecraft.player;
         if (otherPlayerY > playerForHeightCheck.getY()) {
            continue;
         }
         otherPlayerY = otherPlayer.getY();
         LocalPlayer playerForHeightDifference = ClientAccess.schizoMinecraft.player;
         double heightDifference = otherPlayerY - playerForHeightDifference.getY();
         double verticalDistance = Math.abs(heightDifference);
         if (verticalDistance > 15.0D) {
            continue;
         }
         Vec3 otherPosition = otherPlayer.position();
         Vec3 localPosition = ClientAccess.schizoMinecraft.player.position();
         float horizontalDistance = Vec3Utils.horizontalDistance(otherPosition, localPosition);
         nearestPlayerDistance = Math.min(nearestPlayerDistance, horizontalDistance);
      }

      return nearestPlayerDistance < 5.0F;
   }

   private boolean hasDurabilityFlightCondition() {
      ElytraFlyModule.InfDurabilityChoice durabilityChoice = this.module.infDurabilitySetting.getValue();
      if (durabilityChoice.isDisabled()) {
         return PlayerStateUtils.isFallFlying();
      } else {
         return !EntityUtils.hasCollisionBelow(ClientAccess.schizoMinecraft.player);
      }
   }

   private boolean isMovementKeyDown() {
      KeyMapping forwardKey = ClientAccess.schizoMinecraft.options.keyUp;
      KeyMapping backwardKey = ClientAccess.schizoMinecraft.options.keyDown;
      KeyMapping rightKey = ClientAccess.schizoMinecraft.options.keyRight;
      KeyMapping leftKey = ClientAccess.schizoMinecraft.options.keyLeft;
      KeyMapping sneakKey = ClientAccess.schizoMinecraft.options.keyShift;
      KeyMapping jumpKey = ClientAccess.schizoMinecraft.options.keyJump;
      return forwardKey.isDown() || backwardKey.isDown() || rightKey.isDown()
         || leftKey.isDown() || sneakKey.isDown() || jumpKey.isDown();
   }

   public static void bindModuleReferences(ModuleRegistry registry) {
      autoMaceModule = (AutoMaceModule)registry.getModule(AutoMaceModule.class);
   }
}
