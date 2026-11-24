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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
public enum CrystalDamagePolicy implements ClientAccess, NamedEntry {
   PLACE("Place"),
   HIT("Hit");
   private final String displayName;
   private static final AutoCrystalModule autoCrystal = (AutoCrystalModule)ClientContext.getModule(AutoCrystalModule.class);
   CrystalDamagePolicy(String displayName) {
      this.displayName = displayName;
   }

   public double getMinimumDamage(LivingEntity target) {
      boolean obsidianDropNearTarget = autoCrystal.obsidianDropNearTarget;
      boolean quiverThreatActive = autoCrystal.quiverThreatActive;
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
      if (autoCrystal.isFacePlaceEnabled()) {
         float health = EntityUtils.getHealthWithAbsorption(target);
         Float healthLimit = autoCrystal.facePlaceHealthSetting.getValue();
         if (health <= healthLimit) {
            facePlaceOverride = true;
         }

         if (!facePlaceOverride && target instanceof Player player) {
            float armor = InventoryUtils.getLowestArmorDurabilityPercent(player);
            Integer armorLimit = autoCrystal.facePlaceArmorThresholdSetting.getValue();
            if (armor <= armorLimit.intValue()) {
               facePlaceOverride = true;
            }
         }

         if (!facePlaceOverride && target.isUsingItem()) {
            ItemStack activeStack = target.getUseItem();
            if (activeStack.is(Items.SHIELD) && autoCrystal.facePlaceShieldBreakerSetting.getValue()) {
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

      double placeMinimum = autoCrystal.minPlaceDamageSetting.getValue().floatValue();
      double hitMinimum = autoCrystal.minBreakDamageSetting.getValue().floatValue();
      boolean placeMinimumUsesTargetHealth = autoCrystal.minPlaceDamageSetting.shouldDisplayBoundaryLabel();
      boolean ignoreMiningBlock = autoCrystal.shouldIgnoreMiningBlock();
      boolean alwaysIgnoreMining = autoCrystal.alwaysIgnoreMiningSetting.getValue();

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
      if (autoCrystal.isSelfInvulnerable()) {
         return 999.0;
      }

      Float selectedMaximum;
      if (this == HIT) {
         selectedMaximum = autoCrystal.maxBreakSelfDamageSetting.getValue();
      } else {
         selectedMaximum = autoCrystal.maxPlaceSelfDamageSetting.getValue();
      }

      return selectedMaximum.floatValue();
   }

   public boolean shouldRejectDamage(
      LivingEntity target, double selfDamage, double targetDamage, CrystalLethality lethality
   ) {
      boolean selfHoldingTotem = ClientAccess.schizoMinecraft.player.isHolding(Items.TOTEM_OF_UNDYING);
      boolean selfInvulnerable = autoCrystal.isSelfInvulnerable();
      boolean forceSuicide = autoCrystal.forceSuicideSetting.getValue();
      boolean antiSuicide = autoCrystal.antiSuicideSetting.getValue();
      double selfHealth = EntityUtils.getPlayerHealthWithAbsorption();
      double targetHealth = EntityUtils.getHealthWithAbsorption(target);
      double minimumTargetDamage = this.getMinimumDamage(target);
      double maximumSelfDamage = this.getMaximumSelfDamage();
      double minimumRatio = autoCrystal.placeDamageRatioSetting.getValue().floatValue();

      CrystalDamageSafetyInputs inputs = new CrystalDamageSafetyInputs(
         this == HIT,
         selfInvulnerable,
         forceSuicide,
         antiSuicide,
         selfHoldingTotem,
         lethality.isKill(),
         lethality.isIntersection(),
         selfHealth,
         targetHealth,
         minimumTargetDamage,
         maximumSelfDamage,
         minimumRatio
      );
      return inputs.shouldReject(selfDamage, targetDamage);
   }

   @Override
   public String getName() {
      return this.displayName;
   }
}
