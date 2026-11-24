/*
 * ─────────────────────────────────────────────
 * Licensed under the MIT License.
 *
 * Zio is not affiliated with or endorsed by Mio.
 * Provided "as is", without warranty of any kind.
 * ─────────────────────────────────────────────
 */

package dev.schizo.client.utils.combat;

import dev.schizo.client.utils.inventory.EnchantmentUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Data-only input snapshot for one target's explosion-damage calculation.
 *
 * <p>The capture factory reads Minecraft state on the client thread. The
 * resulting value contains only primitives, UUID/String metadata, packed
 * block positions, and immutable reduction records. The calculate method
 * reads only this value and an immutable ExplosionRaycastSnapshot.</p>
 */
public record ExplosionDamageSnapshot(
   int targetEntityId,
   UUID targetUuid,
   long worldGeneration,
   long captureTick,
   String dimensionId,
   double minX,
   double minY,
   double minZ,
   double maxX,
   double maxY,
   double maxZ,
   double explosionX,
   double explosionY,
   double explosionZ,
   double normalizedDistance,
   double power,
   ExplosionDamageMath.Difficulty difficulty,
   boolean immune,
   float healthWithAbsorption,
   float armor,
   float toughness,
   boolean hasResistance,
   float resistanceAmplifier,
   boolean assumeBestArmor,
   int protectionScore,
   boolean hasBlockingComponent,
   double blockingAngle,
   List<ExplosionBlockingDamageMath.Reduction> blockingReductions,
   ExplosionRaycastSnapshot.Query rayQuery
) {
   public ExplosionDamageSnapshot {
      Objects.requireNonNull(targetUuid, "targetUuid");
      Objects.requireNonNull(dimensionId, "dimensionId");
      Objects.requireNonNull(difficulty, "difficulty");
      blockingReductions = List.copyOf(
         Objects.requireNonNull(blockingReductions, "blockingReductions")
      );
      Objects.requireNonNull(rayQuery, "rayQuery");
   }

   /**
    * Captures every mutable input used by DamageUtils on the client thread.
    * worldGeneration must be incremented by the caller whenever the client
    * world/session is replaced or invalidated.
    */
   public static ExplosionDamageSnapshot capture(
      ClientLevel level,
      LivingEntity target,
      AABB bounds,
      Vec3 explosionPosition,
      double power,
      boolean ignoreLowResistance,
      Collection<? extends BlockPos> ignoredPositions,
      BlockPos solidPosition,
      boolean assumeBestArmor,
      long worldGeneration
   ) {
      Objects.requireNonNull(level, "level");
      Objects.requireNonNull(target, "target");
      Objects.requireNonNull(bounds, "bounds");
      Objects.requireNonNull(explosionPosition, "explosionPosition");
      Objects.requireNonNull(ignoredPositions, "ignoredPositions");

      Minecraft minecraft = Minecraft.getInstance();
      if (!minecraft.isSameThread()) {
         throw new IllegalStateException("Explosion damage inputs must be captured on the client thread");
      }
      if (minecraft.level != level || target.level() != level) {
         throw new IllegalStateException("Explosion damage inputs must belong to the active client world");
      }

      double bottomCenterX = bounds.minX + (bounds.maxX - bounds.minX) * 0.5D;
      double bottomCenterY = bounds.minY;
      double bottomCenterZ = bounds.minZ + (bounds.maxZ - bounds.minZ) * 0.5D;
      double normalizedDistance = new Vec3(bottomCenterX, bottomCenterY, bottomCenterZ)
         .distanceTo(explosionPosition) / (power * 2.0D);

      boolean immune = target == minecraft.player && minecraft.player.isCreative();
      ExplosionDamageMath.Difficulty difficulty = switch (level.getDifficulty()) {
         case PEACEFUL -> ExplosionDamageMath.Difficulty.PEACEFUL;
         case EASY -> ExplosionDamageMath.Difficulty.EASY;
         case HARD -> ExplosionDamageMath.Difficulty.HARD;
         default -> ExplosionDamageMath.Difficulty.NORMAL;
      };

      float armor = target.getArmorValue();
      float toughness = (float)target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
      boolean hasResistance = target.hasEffect(MobEffects.RESISTANCE);
      float resistanceAmplifier = 1.0F;
      if (hasResistance || assumeBestArmor) {
         MobEffectInstance resistance = target.getEffect(MobEffects.RESISTANCE);
         if (resistance != null) {
            resistanceAmplifier = resistance.getAmplifier();
         }
      }

      int protectionScore = 0;
      if (target instanceof Player player) {
         int assumedArmorUnits = 1;
         List<ItemStack> armorStacks = dev.schizo.client.utils.inventory.EquipmentUtils.getArmorStacks(player);
         for (ItemStack armorStack : armorStacks) {
            if (armorStack.isEmpty()) {
               continue;
            }

            if (armorStack.is(ItemTags.HEAD_ARMOR)) {
               assumedArmorUnits--;
            }
            Map enchantments = EnchantmentUtils.getLevels(armorStack);
            Integer blastProtection = (Integer)enchantments.getOrDefault(Enchantments.BLAST_PROTECTION, 0);
            Integer protection = (Integer)enchantments.getOrDefault(Enchantments.PROTECTION, 0);
            protectionScore += blastProtection * 2 + protection;
            assumedArmorUnits++;
         }

         if (assumeBestArmor) {
            float assumedArmorFraction = (float)assumedArmorUnits / 4.0F;
            protectionScore = (int)(assumedArmorFraction * 20.0F);
         }
      }
      if (protectionScore > 20) {
         protectionScore = 20;
      }

      boolean hasBlockingComponent = false;
      double blockingAngle = 0.0D;
      List<ExplosionBlockingDamageMath.Reduction> blockingReductions = new ArrayList<>();
      if (target.isUsingItem()) {
         ItemStack usedItem = target.getUseItem();
         BlocksAttacks blocksAttacks = (BlocksAttacks)usedItem.get(DataComponents.BLOCKS_ATTACKS);
         if (blocksAttacks != null) {
            hasBlockingComponent = true;
            DamageSources damageSources = level.damageSources();
            DamageSource damageSource = damageSources.explosion((Explosion)null);
            Vec3 viewVector = target.calculateViewVector(0.0F, target.getYHeadRot());
            Vec3 attackDirection = explosionPosition.subtract(target.position());
            Vec3 horizontalDirection = new Vec3(attackDirection.x, 0.0D, attackDirection.z).normalize();
            blockingAngle = Math.acos(horizontalDirection.dot(viewVector));

            for (BlocksAttacks.DamageReduction reduction : blocksAttacks.damageReductions()) {
               boolean damageTypeMatches = reduction.type().isEmpty()
                  || reduction.type().get().contains(damageSource.typeHolder());
               blockingReductions.add(new ExplosionBlockingDamageMath.Reduction(
                  reduction.horizontalBlockingAngle(),
                  damageTypeMatches,
                  reduction.base(),
                  reduction.factor()
               ));
            }
         }
      }

      String dimensionId = level.dimension().identifier().toString();
      return new ExplosionDamageSnapshot(
         target.getId(),
         target.getUUID(),
         worldGeneration,
         level.getGameTime(),
         dimensionId,
         bounds.minX,
         bounds.minY,
         bounds.minZ,
         bounds.maxX,
         bounds.maxY,
         bounds.maxZ,
         explosionPosition.x,
         explosionPosition.y,
         explosionPosition.z,
         normalizedDistance,
         power,
         difficulty,
         immune,
         target.getHealth() + target.getAbsorptionAmount(),
         armor,
         toughness,
         hasResistance,
         resistanceAmplifier,
         assumeBestArmor,
         protectionScore,
         hasBlockingComponent,
         blockingAngle,
         blockingReductions,
         ExplosionRaycastSnapshot.Query.fromPositions(
            ignoredPositions,
            solidPosition,
            ignoreLowResistance
         )
      );
   }

   /**
    * Performs the full calculation using only captured values. A mismatched
    * ray snapshot yields an incomplete result and must not be used to rank a
    * placement candidate.
    */
   public DamageResult calculate(ExplosionRaycastSnapshot raySnapshot) {
      Objects.requireNonNull(raySnapshot, "raySnapshot");
      if (raySnapshot.worldGeneration() != this.worldGeneration
         || raySnapshot.captureTick() != this.captureTick
         || !raySnapshot.dimensionId().equals(this.dimensionId)
         || !raySnapshot.matchesCollisionEntity(this.targetEntityId, this.targetUuid)) {
         return DamageResult.incomplete();
      }

      if (this.immune || this.normalizedDistance > 1.0D) {
         return new DamageResult(0.0F, 0.0F, 0, true);
      }

      ExplosionExposureMath.ExposureResult exposure = ExplosionExposureMath.calculateFromSnapshot(
         this.minX,
         this.minY,
         this.minZ,
         this.maxX,
         this.maxY,
         this.maxZ,
         this.explosionX,
         this.explosionY,
         this.explosionZ,
         raySnapshot,
         this.rayQuery
      );
      if (!exposure.complete()) {
         return DamageResult.incomplete();
      }

      float damage = ExplosionDamageMath.calculate(
         this.normalizedDistance,
         exposure.exposure(),
         this.power,
         this.difficulty
      );
      damage = ExplosionDamageReductionMath.apply(
         damage,
         this.armor,
         this.toughness,
         this.hasResistance,
         this.resistanceAmplifier,
         this.assumeBestArmor,
         this.protectionScore
      );
      if (this.hasBlockingComponent) {
         damage = ExplosionBlockingDamageMath.apply(
            damage,
            this.blockingAngle,
            this.blockingReductions
         );
      }

      return new DamageResult(
         Math.max(damage, 0.0F),
         exposure.exposure(),
         exposure.totalSamples(),
         true
      );
   }

   public record DamageResult(
      float damage,
      float exposure,
      int exposureSamples,
      boolean complete
   ) {
      public static DamageResult incomplete() {
         return new DamageResult(0.0F, 0.0F, 0, false);
      }
   }
}
