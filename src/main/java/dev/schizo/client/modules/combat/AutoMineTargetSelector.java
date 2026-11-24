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
import dev.schizo.client.managers.RotationManager;
import dev.schizo.client.utils.combat.BoundingBoxUtils;
import dev.schizo.client.utils.entity.PlayerStateUtils;
import dev.schizo.client.utils.network.ServerSideDeathState;

import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public final class AutoMineTargetSelector implements ClientAccess {
   private final AutoMineModule module;


   public AABB getTargetFootprintBounds(Player target) {
      AABB boundsSnapshot = target.getBoundingBox();
      double shrink = -BoundingBoxUtils.blockCornerInset;
      boundsSnapshot = boundsSnapshot.inflate(shrink, 0.0, shrink);
      double maximumY = target.getY();
      return boundsSnapshot.setMaxY(maximumY);
   }

   AutoMineTargetSelector(AutoMineModule module) {
      this.module = module;
   }


   public Player selectTarget() {
      Stream<AbstractClientPlayer> filteredCandidates = ClientAccess.schizoMinecraft.level.players().stream()
         .filter(candidate -> CombatTargetPolicy.isEligible(
            candidate.isAlive(),
            candidate.isDeadOrDying(),
            candidate == ClientAccess.schizoMinecraft.player,
            candidate.isSpectator(),
            candidate.isCreative(),
            ClientContext.socialRegistry.isFriendPlayer(candidate),
            ((ServerSideDeathState)candidate).isServerSideDead()
         ) && ClientAccess.schizoMinecraft.player.distanceTo(candidate) <= this.module.getRange() + 2.0F)
         .filter(candidate -> !this.module.ignoreNakedSetting.getValue() || PlayerStateUtils.hasArmor(candidate));
      Comparator<AbstractClientPlayer> yawOrder = Comparator.comparing((Function<AbstractClientPlayer, Float>)candidate -> {
            float yawSnapshot = ClientAccess.schizoMinecraft.player.getYRot();

            float candidateYaw = RotationManager.getRotationsToEntity(candidate)[0];

            float yawDifference = Mth.degreesDifferenceAbs(yawSnapshot, candidateYaw);
            return yawDifference;
         });
      return filteredCandidates.min(yawOrder).orElse(null);
   }
}
