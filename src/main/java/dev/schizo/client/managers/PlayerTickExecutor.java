/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.managers;

import dev.schizo.client.access.LocalPlayerTickAccess;

import dev.schizo.client.ClientAccess;

import dev.schizo.mixin.LivingEntityAccessor;
import net.minecraft.world.phys.Vec3;

public final class PlayerTickExecutor implements ClientAccess {
   private static boolean runningExtraTick;
   private PlayerTickExecutor() {
   }

   public static boolean isRunningExtraTick() {
      return runningExtraTick;
   }

   public static void runExtraTick() {
      runningExtraTick = true;
      int useItemRemainingTicks = ClientAccess.schizoMinecraft.player.getUseItemRemainingTicks();
      float moveDistance = ClientAccess.schizoMinecraft.player.moveDist;
      float walkAnimationSpeed = ClientAccess.schizoMinecraft.player.walkAnimation.speed();
      float walkDistance = ClientAccess.schizoMinecraft.player.avatarState().walkDist;
      float previousWalkDistance = ClientAccess.schizoMinecraft.player.avatarState().walkDistO;
      float verticalBob = ClientAccess.schizoMinecraft.player.yBob;
      float previousVerticalBob = ClientAccess.schizoMinecraft.player.yBobO;
      float horizontalBob = ClientAccess.schizoMinecraft.player.xBob;
      float previousHorizontalBob = ClientAccess.schizoMinecraft.player.xBobO;
      Vec3 previousPosition = new Vec3(
         ClientAccess.schizoMinecraft.player.xo,
         ClientAccess.schizoMinecraft.player.yo,
         ClientAccess.schizoMinecraft.player.zo
      );
      int attackStrengthTicks = ClientAccess.schizoMinecraft.player.attackStrengthTicker;
      int swingTime = ClientAccess.schizoMinecraft.player.swingTime;
      float previousAttackAnimation = ClientAccess.schizoMinecraft.player.oAttackAnim;
      float attackAnimation = ClientAccess.schizoMinecraft.player.attackAnim;
      LocalPlayerTickAccess baseTickPlayer = (LocalPlayerTickAccess)ClientAccess.schizoMinecraft.player;
      baseTickPlayer.superTick();
      LocalPlayerTickAccess eventResetPlayer = (LocalPlayerTickAccess)ClientAccess.schizoMinecraft.player;
      eventResetPlayer.resetEvent();
      ClientAccess.schizoMinecraft.player.attackStrengthTicker = attackStrengthTicks;
      ((LivingEntityAccessor)ClientAccess.schizoMinecraft.player)
         .setItemUseTimeLeft(useItemRemainingTicks);
      ClientAccess.schizoMinecraft.player.moveDist = moveDistance;
      ClientAccess.schizoMinecraft.player.walkAnimation.setSpeed(walkAnimationSpeed);
      ClientAccess.schizoMinecraft.player.avatarState().walkDist = walkDistance;
      ClientAccess.schizoMinecraft.player.avatarState().walkDistO = previousWalkDistance;
      ClientAccess.schizoMinecraft.player.yBob = verticalBob;
      ClientAccess.schizoMinecraft.player.yBobO = previousVerticalBob;
      ClientAccess.schizoMinecraft.player.xBob = horizontalBob;
      ClientAccess.schizoMinecraft.player.xBobO = previousHorizontalBob;
      ClientAccess.schizoMinecraft.player.swingTime = swingTime;
      ClientAccess.schizoMinecraft.player.oAttackAnim = previousAttackAnimation;
      ClientAccess.schizoMinecraft.player.attackAnim = attackAnimation;
      LocalPlayerTickAccess movementPacketPlayer = (LocalPlayerTickAccess)ClientAccess.schizoMinecraft.player;
      movementPacketPlayer.sendMovementPacketsWrapper();
      LocalPlayerTickAccess rotationResetPlayer = (LocalPlayerTickAccess)ClientAccess.schizoMinecraft.player;
      rotationResetPlayer.resetRotations();
      ClientAccess.schizoMinecraft.player.xo = previousPosition.x;
      ClientAccess.schizoMinecraft.player.yo = previousPosition.y;
      ClientAccess.schizoMinecraft.player.zo = previousPosition.z;
      runningExtraTick = false;
   }

}
