/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.modules.combat;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.utils.combat.BoundingBoxUtils;
import dev.schizo.client.utils.entity.EntityUtils;
import dev.schizo.client.utils.entity.PlayerStateUtils;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.entity.LivingEntity;

public record CrystalTargetDamage(
   LivingEntity target,
   double damage,
   CrystalPlacementKind kind,
   CrystalLethality lethality
) {
   static CrystalTargetDamage calculateForBase(
      AutoCrystalModule module,
      BlockPos base,
      boolean hasCrystalBase,
      List<LivingEntity> targets
   ) {
      CrystalTargetDamage bestTargetDamage = null;
      for (LivingEntity candidate : targets) {
         if (candidate == null || module.isInvalidTarget(candidate)) {
            continue;
         }

         double baseY = base.getY();
         double targetY = candidate.getY();
         if (baseY > targetY + 1.5D && !hasCrystalBase) {
            continue;
         }

         AABB targetBounds = module.getTargetBounds(candidate);
         double minimumDamage = CrystalDamagePolicy.PLACE.getMinimumDamage(candidate);
         double damage = module.calculateExplosionDamage(base, candidate, targetBounds, true);
         CrystalLethality lethality = CrystalLethality.classifyLethality(candidate, damage);
         boolean holdingCrystal = candidate.isHolding(Items.END_CRYSTAL);

         boolean ignoreNakeds = module.ignoreNakedsSetting.getValue();
         boolean targetHasArmor = !ignoreNakeds || PlayerStateUtils.hasArmor(candidate);

         Vec3 boundsCenter = BoundingBoxUtils.getBottomCenter(targetBounds);
         Vec3 baseCenter = Vec3.atBottomCenterOf(base);
         double distanceToBase = boundsCenter.distanceTo(baseCenter);

         boolean friendEntity = ClientContext.socialRegistry.isFriendEntity(candidate)
            && candidate != ClientAccess.schizoMinecraft.player;
         boolean antiFriendKill = friendEntity && module.antiFriendKillSetting.getValue();
         double targetHealth = antiFriendKill
            ? EntityUtils.getHealthWithAbsorption(candidate)
            : Double.POSITIVE_INFINITY;

         CrystalTargetSelectionPolicy.Decision decision =
            CrystalTargetSelectionPolicy.evaluateCandidate(
               new CrystalTargetSelectionPolicy.CandidateState(
                  baseY,
                  targetY,
                  hasCrystalBase,
                  ignoreNakeds,
                  targetHasArmor,
                  holdingCrystal,
                  distanceToBase,
                  module.crystalRangeSetting.getValue().doubleValue(),
                  lethality.lethalDamage,
                  module.obsidianDropNearTarget,
                  module.quiverThreatActive,
                  friendEntity,
                  antiFriendKill,
                  damage,
                  targetHealth,
                  minimumDamage
               )
            );

         if (decision == CrystalTargetSelectionPolicy.Decision.ABORT_SCAN) {
            return null;
         }
         if (decision == CrystalTargetSelectionPolicy.Decision.REJECT) {
            continue;
         }
         if (bestTargetDamage != null
            && !CrystalTargetSelectionPolicy.shouldReplaceBest(damage, bestTargetDamage.damage())) {
            continue;
         }

         bestTargetDamage = new CrystalTargetDamage(
            candidate,
            damage,
            CrystalPlacementKind.NONE,
            lethality
         );
      }

      return bestTargetDamage;
   }
}
