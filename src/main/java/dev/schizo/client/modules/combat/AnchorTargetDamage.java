/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.combat;

import dev.schizo.client.utils.combat.DamageUtils;
import dev.schizo.client.utils.math.MathConstants;
import java.util.List;
import java.util.Iterator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.utils.network.ServerSideDeathState;

import net.minecraft.world.entity.player.Player;

public record AnchorTargetDamage(
   Player target,
   double damage
) implements ClientAccess {
   static AnchorTargetDamage calculateForBase(AnchorAuraModule module, BlockPos position) {
      boolean ignoreMiningBlock = module.shouldIgnoreMiningBlock();
      AnchorTargetDamage bestDamage = null;

      List playerList = ClientAccess.schizoMinecraft.level.players();
      Iterator players = playerList.iterator();

      while (players.hasNext()) {
         if (Thread.currentThread().isInterrupted()) {
            return null;
         }
         Player candidate = (Player)players.next();

         double validationRange = (module.targetRangeSetting.getValue()).floatValue();
         if (!AnchorTargetDamage.isInvalidTarget(candidate, validationRange)) {
            Vec3 explosionPositionSnapshot = position.getCenter();
            AABB boundsSnapshot = candidate.getBoundingBox();

            BlockPos ignoredBaseSnapshot = !module.isAnchorBlock(position)
                  && !ignoreMiningBlock
               ? null
               : position;
            double damage = DamageUtils.getExplosionDamage(
               explosionPositionSnapshot, candidate, boundsSnapshot, 5.0D, true, ignoredBaseSnapshot, (BlockPos)null
            );

            if (!((module.minDamageSetting.getValue()).floatValue() > damage)
               && !(damage < MathConstants.ONE_HALF)) {
               if (bestDamage != null) {
                  if (!(damage > bestDamage.damage())) {
                     continue;
                  }
               }

               bestDamage = new AnchorTargetDamage(candidate, damage);
            }
         }
      }

      return bestDamage;
   }

   public static boolean isInvalidTarget(Player target, double range) {
      if (target == null || target == ClientAccess.schizoMinecraft.player || !target.isAlive()) {
         return true;
      }

      if (ClientContext.socialRegistry.isFriendPlayer(target) || ((ServerSideDeathState)target).isServerSideDead()) {
         return true;
      }
      if (target.isSpectator() || target.getAbilities().instabuild) {
         return true;
      }

      return ClientAccess.schizoMinecraft.player.distanceTo(target) > range;
   }
}
