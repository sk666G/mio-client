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
import dev.schizo.client.utils.entity.EntityUtils;
import dev.schizo.client.utils.inventory.InventoryUtils;
import dev.schizo.client.settings.NamedEntry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
public enum CrystalDamagePolicy implements ClientAccess, NamedEntry {
   PLACE("Place"),
   HIT("Hit");
   private final String displayName;

   CrystalDamagePolicy(String displayName) {
      this.displayName = displayName;
   }

   public double getMinimumDamage(LivingEntity target) {
      boolean obsidianDropNearTarget = schizo$autoCrystal().obsidianDropNearTarget;
      boolean quiverThreatActive = schizo$autoCrystal().quiverThreatActive;
      if (obsidianDropNearTarget || quiverThreatActive) {
         return new CrystalDamageThresholds(
            obsidianDropNearTarget,
            quiverThreatActive,
            false,
            false,
            EntityUtils.getHealthWithAbsorption(target),
            0.0D,
            0.0D,
            false,
            false,
            false
         ).minimumDamage(this == HIT);
      }

      boolean facePlaceOverride = false;
      if (schizo$autoCrystal().isFacePlaceEnabled()) {
         float health = EntityUtils.getHealthWithAbsorption(target);
         Float healthLimit = schizo$autoCrystal().facePlaceHealthSetting.getValue();
         if (health <= healthLimit) {
            facePlaceOverride = true;
         }

         if (!facePlaceOverride && target instanceof Player player) {
            float armor = InventoryUtils.getLowestArmorDurabilityPercent(player);
            Integer armorLimit = schizo$autoCrystal().facePlaceArmorThresholdSetting.getValue();
            if (armor <= armorLimit.intValue()) {
               facePlaceOverride = true;
            }
         }

         if (!facePlaceOverride && target.isUsingItem()) {
            ItemStack activeStack = target.getUseItem();
            if (activeStack.is(Items.SHIELD) && schizo$autoCrystal().facePlaceShieldBreakerSetting.getValue()) {
               facePlaceOverride = true;
            }
         }
      }

      double targetHealth = EntityUtils.getHealthWithAbsorption(target);
      if (facePlaceOverride) {
         return new CrystalDamageThresholds(
            false, false, true, false, targetHealth,
            0.0D, 0.0D, false, false, false
         ).minimumDamage(this == HIT);
      }

      BlockState stateSnapshot = ClientAccess.schizoMinecraft.level.getBlockState(target.blockPosition());
      boolean targetInNetherPortal = stateSnapshot.is(Blocks.NETHER_PORTAL);
      if (targetInNetherPortal) {
         return new CrystalDamageThresholds(
            false, false, false, true, targetHealth,
            0.0D, 0.0D, false, false, false
         ).minimumDamage(this == HIT);
      }

      double placeMinimum = schizo$autoCrystal().minPlaceDamageSetting.getValue().floatValue();
      double hitMinimum = schizo$autoCrystal().minBreakDamageSetting.getValue().floatValue();
      boolean placeMinimumUsesTargetHealth = schizo$autoCrystal().minPlaceDamageSetting.shouldDisplayBoundaryLabel();
      boolean ignoreMiningBlock = schizo$autoCrystal().shouldIgnoreMiningBlock();
      boolean alwaysIgnoreMining = schizo$autoCrystal().alwaysIgnoreMiningSetting.getValue();

      CrystalDamageThresholds thresholds = new CrystalDamageThresholds(
         false,
         false,
         false,
         false,
         targetHealth,
         placeMinimum,
         hitMinimum,
         placeMinimumUsesTargetHealth,
         ignoreMiningBlock,
         alwaysIgnoreMining
      );
      return thresholds.minimumDamage(this == HIT);
   }

   public double getMaximumSelfDamage() {
      if (schizo$autoCrystal().isSelfInvulnerable()) {
         return 999.0;
      }

      Float selectedMaximum;
      if (this == HIT) {
         selectedMaximum = schizo$autoCrystal().maxBreakSelfDamageSetting.getValue();
      } else {
         selectedMaximum = schizo$autoCrystal().maxPlaceSelfDamageSetting.getValue();
      }

      return selectedMaximum.floatValue();
   }

   public boolean shouldRejectDamage(
      LivingEntity target, double selfDamage, double targetDamage, CrystalLethality lethality
   ) {
      boolean selfHoldingTotem = ClientAccess.schizoMinecraft.player.isHolding(Items.TOTEM_OF_UNDYING);
      if (schizo$autoCrystal().isSelfInvulnerable()) {
         selfDamage = 0.0D;
      }

      // Preserve the original early return: Force Suicide must not read the
      // anti-suicide, health, minimum-damage or ratio settings below.
      if (schizo$autoCrystal().forceSuicideSetting.getValue()) {
         return false;
      }

      double selfHealth = EntityUtils.getPlayerHealthWithAbsorption();
      if (selfDamage >= selfHealth && schizo$autoCrystal().antiSuicideSetting.getValue()
         && CrystalDamageSafetyInputs.shouldRejectAntiSuicide(
            selfDamage,
            selfHealth,
            true,
            lethality.isKill(),
            selfHoldingTotem
         )) {
         return true;
      }

      boolean targetKill = lethality.isKill();
      boolean targetIntersection = lethality.isIntersection();
      if (CrystalDamageSafetyInputs.shouldAcceptHitIntersection(this == HIT, targetIntersection)
         || targetKill) {
         return false;
      }

      // The ratio applies only to PLACE and only with positive self damage.
      if (this == PLACE && selfDamage > 0.0D) {
         double minimumRatio = schizo$autoCrystal().placeDamageRatioSetting.getValue().floatValue();
         if (CrystalDamageSafetyInputs.shouldRejectPlaceRatio(
            false,
            selfDamage,
            targetDamage,
            minimumRatio
         )) {
            return true;
         }
      }

      double targetHealth = EntityUtils.getHealthWithAbsorption(target);
      if (targetDamage >= targetHealth) {
         return false;
      }

      double minimumTargetDamage = this.getMinimumDamage(target);
      if (!(targetDamage >= minimumTargetDamage)) {
         return true;
      }

      double maximumSelfDamage = this.getMaximumSelfDamage();
      return !(selfDamage <= maximumSelfDamage);
   }

   @Override
   public String getName() {
      return this.displayName;
   }

   private static AutoCrystalModule schizo$autoCrystal() {
      return ClientContext.requireModule(AutoCrystalModule.class);
   }
}
