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
import dev.schizo.client.utils.network.ServerSideDeathState;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

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
      LocalPlayer localPlayer = ClientAccess.schizoMinecraft.player;
      boolean forceSuicide = module.forceSuicideSetting.getValue();
      double targetRange = forceSuicide ? 0.0D : module.targetRangeSetting.getValue().doubleValue();

      for (LivingEntity candidate : targets) {
         if (candidate == null) {
            continue;
         }

         boolean localCandidate = candidate == localPlayer;
         CrystalTargetSelectionPolicy.TargetState targetState;
         if (forceSuicide) {
            // isInvalidTarget deliberately bypasses all ordinary checks in
            // ForceSuicide mode and permits only the local player.
            targetState = new CrystalTargetSelectionPolicy.TargetState(
               localCandidate, false, false, false, true, false, false, false, 0.0D
            );
         } else {
            boolean playerCandidate = candidate instanceof Player;
            boolean friendPlayer = playerCandidate
               && ClientContext.socialRegistry.isFriendPlayer((Player)candidate);
            boolean creative = playerCandidate && ((Player)candidate).isCreative();
            targetState = new CrystalTargetSelectionPolicy.TargetState(
               localCandidate,
               playerCandidate,
               friendPlayer,
               creative,
               candidate.isAlive(),
               candidate.isDeadOrDying(),
               ((ServerSideDeathState)candidate).isServerSideDead(),
               candidate.isSpectator(),
               candidate.distanceTo(localPlayer)
            );
         }

         if (CrystalTargetSelectionPolicy.isInvalidTarget(targetState, forceSuicide, targetRange)) {
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
            && !localCandidate;
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
