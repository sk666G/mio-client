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
import dev.schizo.client.managers.SocialRegistry;
import dev.schizo.client.utils.world.BlockInteractionUtils;
import dev.schizo.client.utils.entity.PlayerStateUtils;
import dev.schizo.client.utils.network.ServerSideDeathState;
import dev.schizo.client.modules.ModuleCategory;
import dev.schizo.client.settings.BooleanSetting;
import dev.schizo.client.settings.NumberSetting;
import dev.schizo.client.settings.Setting;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class SelfTrapModule extends AutoTrapModule {
   private Setting<Boolean> onlyHoleSetting;
   private Setting<Boolean> onlyStandingSetting;
   private Setting<Boolean> smartSetting;
   protected Setting<Float> enemyRangeSetting;

   @Override
   public String getInfo() {
      return null;
   }

   private void initializeSelfTrapSettings() {
      BooleanSetting onlyHoleEditor = new BooleanSetting("OnlyHole", false);
      this.onlyHoleSetting = this.add(onlyHoleEditor);

      BooleanSetting onlyStandingEditor = new BooleanSetting("OnlyStanding", false);
      this.onlyStandingSetting = this.add(onlyStandingEditor);

      BooleanSetting smartEditor = new BooleanSetting("Smart", false);
      Setting<Boolean> smartDependency = smartEditor.markVisibilityDependency();
      this.smartSetting = this.add(smartDependency);

      NumberSetting<Float> enemyRange = new NumberSetting<>(
         "EnemyRange",
         3.0F,
         1.0F,
         6.0F, unused -> this.smartSetting.isExpanded()
      );
      NumberSetting<Float> enemyRangeWithUnit = enemyRange.withUnit("m");
      this.enemyRangeSetting = this.add(enemyRangeWithUnit);
   }

   @Override
   protected boolean shouldPlaceBelow() {
      return false;
   }

   private Entity findNearestEnemy() {
      Iterable<Entity> entities = ClientAccess.schizoMinecraft.level.entitiesForRendering();
      Spliterator<Entity> entitySpliterator = entities.spliterator();
      Stream<Entity> candidateStream = StreamSupport.stream(entitySpliterator, false).filter(candidate -> {
         if (!(candidate instanceof Player player)) {
            return false;
         }

         SocialRegistry friends = ClientContext.socialRegistry;
         String name = candidate.getName().getString();
         boolean friend = friends.isFriendName(name);
         if (!CombatTargetPolicy.isEligible(
            player.isAlive(),
            player.isDeadOrDying(),
            player == ClientAccess.schizoMinecraft.player,
            player.isSpectator(),
            player.isCreative(),
            friend,
            ((ServerSideDeathState)player).isServerSideDead()
         )) {
            return false;
         }

         float candidateDistance = ClientAccess.schizoMinecraft.player.distanceTo(player);
         return candidateDistance <= this.enemyRangeSetting.getValue();
      });
      LocalPlayer distancePlayer = ClientAccess.schizoMinecraft.player;
      Comparator<Entity> nearestFirst = Comparator.comparing(distancePlayer::distanceToSqr);
      Optional<Entity> enemy = candidateStream.min(nearestFirst);
      return enemy.orElse(null);
   }

   private boolean shouldSkipUnsupportedPlacement(BlockPos blockPos) {
      boolean attackCheckFlag = true;

      if (!BlockInteractionUtils.canPlaceAt(blockPos, true, attackCheckFlag)) {
         return true;
      }

      Setting<Boolean> airPlaceSettingRef = this.placementAirPlaceSetting;

      if (airPlaceSettingRef.getValue()) {
         return false;
      }

      boolean strictDirectionSnapshot = BlockInteractionUtils.isStrictDirectionEnabled();
      Setting<Boolean> raytraceSettingRef = this.placementRaytraceSetting;

      boolean raytrace = raytraceSettingRef.getValue();
      return BlockInteractionUtils.findSupportDirection(blockPos, strictDirectionSnapshot, raytrace) == null;
   }

   @Override
   protected Player selectTarget() {
      return ClientAccess.schizoMinecraft.player;
   }

   @Override
   public List getPlacementPositions() {
      List placements = super.getPlacementPositions();
      placements.removeIf((java.util.function.Predicate<net.minecraft.core.BlockPos>)this::shouldSkipUnsupportedPlacement);

      if (this.onlyStandingSetting.getValue() && PlayerStateUtils.isLocalHeightBelowOne()) {
         return Collections.emptyList();
      }
      if (this.smartSetting.getValue()) {
         if (this.findNearestEnemy() == null) {
            return Collections.emptyList();
         }
      }
      if (this.onlyHoleSetting.getValue()) {
         if (!ClientContext.holeManager.isInHole()) {
            return Collections.emptyList();
         }
      }
      return placements;
   }

   public SelfTrapModule() {
      super("SelfTrap", "Covers you with blocks.", ModuleCategory.COMBAT);

      this.setDisableOnRespawn(true);

      this.initializeSelfTrapSettings();
      Setting<Float> range = this.rangeSetting;
      this.unregister(range);
      this.unregister(this.ignoreNakedsSetting);
      this.unregister(this.ignoreCrawlSetting);
      this.unregister(this.onlyPhasedSetting);
      this.unregister(this.logoutsSetting);
      this.unregister(this.belowSetting);
   }
}
