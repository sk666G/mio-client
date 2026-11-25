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
      var player = ClientAccess.schizoMinecraft.player;
      if (player == null) {
         return;
      }

      runningExtraTick = true;
      int useItemRemainingTicks = player.getUseItemRemainingTicks();
      float moveDistance = player.moveDist;
      float walkAnimationSpeed = player.walkAnimation.speed();
      float walkDistance = player.avatarState().walkDist;
      float previousWalkDistance = player.avatarState().walkDistO;
      float verticalBob = player.yBob;
      float previousVerticalBob = player.yBobO;
      float horizontalBob = player.xBob;
      float previousHorizontalBob = player.xBobO;
      Vec3 previousPosition = new Vec3(player.xo, player.yo, player.zo);
      int attackStrengthTicks = player.attackStrengthTicker;
      int swingTime = player.swingTime;
      float previousAttackAnimation = player.oAttackAnim;
      float attackAnimation = player.attackAnim;
      LocalPlayerTickAccess tickAccess = (LocalPlayerTickAccess)player;

      try {
         tickAccess.superTick();
         tickAccess.resetEvent();

         player.attackStrengthTicker = attackStrengthTicks;
         ((LivingEntityAccessor)player).setItemUseTimeLeft(useItemRemainingTicks);
         player.moveDist = moveDistance;
         player.walkAnimation.setSpeed(walkAnimationSpeed);
         player.avatarState().walkDist = walkDistance;
         player.avatarState().walkDistO = previousWalkDistance;
         player.yBob = verticalBob;
         player.yBobO = previousVerticalBob;
         player.xBob = horizontalBob;
         player.xBobO = previousHorizontalBob;
         player.swingTime = swingTime;
         player.oAttackAnim = previousAttackAnimation;
         player.attackAnim = attackAnimation;

         tickAccess.sendMovementPacketsWrapper();
         tickAccess.resetRotations();
      } finally {
         player.attackStrengthTicker = attackStrengthTicks;
         ((LivingEntityAccessor)player).setItemUseTimeLeft(useItemRemainingTicks);
         player.moveDist = moveDistance;
         player.walkAnimation.setSpeed(walkAnimationSpeed);
         player.avatarState().walkDist = walkDistance;
         player.avatarState().walkDistO = previousWalkDistance;
         player.yBob = verticalBob;
         player.yBobO = previousVerticalBob;
         player.xBob = horizontalBob;
         player.xBobO = previousHorizontalBob;
         player.swingTime = swingTime;
         player.oAttackAnim = previousAttackAnimation;
         player.attackAnim = attackAnimation;
         player.xo = previousPosition.x;
         player.yo = previousPosition.y;
         player.zo = previousPosition.z;
         runningExtraTick = false;
      }
   }

}
