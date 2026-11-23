/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils.movement;

import dev.schizo.client.integrations.ProtocolCompatibility;
import dev.schizo.client.integrations.ProtocolVersionAccess;

import dev.schizo.client.ClientAccess;

import dev.schizo.mixin.LocalPlayerAccessor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;

public final class MovementInputProcessor implements ClientAccess {
   public void applyInputPreservingBobbing() {
      float previousYawBob = ClientAccess.schizoMinecraft.player.yBobO;
      float previousPitchBob = ClientAccess.schizoMinecraft.player.xBobO;
      float pitchBob = ClientAccess.schizoMinecraft.player.xBob;
      float yawBob = ClientAccess.schizoMinecraft.player.yBob;
      LocalPlayer player = ClientAccess.schizoMinecraft.player;

      player.applyInput();
      ClientAccess.schizoMinecraft.player.yBobO = previousYawBob;
      ClientAccess.schizoMinecraft.player.xBobO = previousPitchBob;
      ClientAccess.schizoMinecraft.player.xBob = pitchBob;
      ClientAccess.schizoMinecraft.player.yBob = yawBob;
   }

   public Vec2 normalizeInput(Vec2 input) {
      ProtocolVersionAccess targetVersion = ProtocolCompatibility.getTargetVersion();
      if (targetVersion.olderEq_1_21_4()) {
         return ((LocalPlayerAccessor)ClientAccess.schizoMinecraft.player)
            .schizo$applyMovementSpeedFactors(input);
      }

      return input.normalized();
   }

   public MovementDirection getRelativeDirection(float movementYaw, float rotationYaw) {
      double difference = (Mth.degreesDifference(movementYaw, rotationYaw) + 360.0D) % 360.0D;
      MovementDirection[] directions = MovementDirection.values();
      return directions[(int)Math.round(difference / 45.0D) % directions.length];
   }
}
