/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.movement;

import static dev.schizo.client.utils.math.MathConstants.FULL_TURN_DEGREES;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.LocalPlayerMoveEvent;
import dev.schizo.client.utils.movement.MovementPhysics;
import dev.schizo.client.utils.ElapsedTimer;
import dev.schizo.client.utils.network.PacketSender;
import dev.schizo.client.utils.movement.ElytraFlyModeHandler;

import net.minecraft.core.Direction.Axis;


public class PacketElytraFlyHandler extends ElytraFlyModeHandler {
   private long accelerationStartTimeMillis;
   private float flightSpeed;
   private final ElapsedTimer idleMovementTimer = new ElapsedTimer();

   public PacketElytraFlyHandler(ElytraFlyModule module) {
      super(module);
   }

   @Override
   public void onEnable() {
      this.flightSpeed = 0.0F;
      this.accelerationStartTimeMillis = System.currentTimeMillis();
   }

   @Override
   public void handleMovement(LocalPlayerMoveEvent event) {
      if (!this.module.isPacketFlightActive()) {
         return;
      }
      if (!MovementPhysics.isLocalPlayerMoving()) {
         this.accelerationStartTimeMillis = System.currentTimeMillis();
      }
      if (this.module.accelerateSetting.getValue()) {
         this.flightSpeed = (float)MovementPhysics.accelerateTo(
            this.module.flightSpeedSetting.getValue(), this.module.accelMinSetting.getValue(),
            this.module.accelTimeSetting.getValue(), this.accelerationStartTimeMillis
         );
      } else {
         this.flightSpeed = this.module.flightSpeedSetting.getValue();
      }
      double[] horizontalMotion = MovementPhysics.applyHorizontalMotion(event, this.flightSpeed);
      float glideMotion = -this.module.glideSetting.getValue();
      ClientAccess.schizoMinecraft.player.setDeltaMovement(horizontalMotion[0], glideMotion, horizontalMotion[1]);
      event.setY(glideMotion);
      if (ClientAccess.schizoMinecraft.player.input.keyPresses.shift()) {
         event.setY(-0.5D);
         ClientAccess.schizoMinecraft.player.setDeltaMovement(
            ClientAccess.schizoMinecraft.player.getDeltaMovement().with(Axis.Y, -0.5D)
         );
      }
      PacketSender.sendStartFallFlying();
      if (this.module.antiAfkSetting.getValue() && event.getX() == 0.0 && event.getZ() == 0.0
         && this.idleMovementTimer.hasElapsed(2500L)) {
         double radians = Math.toRadians(ClientAccess.schizoMinecraft.player.tickCount % FULL_TURN_DEGREES);
         event.setHorizontalVelocity(Math.sin(radians) * 0.03D, Math.cos(radians) * 0.03D);
         return;
      }
      if (MovementPhysics.isLocalPlayerMoving()) {
         this.idleMovementTimer.reset();
      }
   }

}
