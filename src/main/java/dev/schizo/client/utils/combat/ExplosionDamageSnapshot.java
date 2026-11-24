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
import dev.schizo.client.utils.inventory.EquipmentUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Reusable, data-only explosion damage profile for one target.
 *
 * <p>The capture factory reads Minecraft state on the client thread. The
 * resulting value contains only primitives, UUID/String metadata and
 * immutable reduction records. It can be reused for many candidate explosion
 * positions in one scan when paired with a ray snapshot captured for the same
 * target collision profile.</p>
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
   double targetX,
   double targetZ,
   double viewX,
   double viewZ,
   ExplosionDamageMath.Difficulty difficulty,
   boolean immune,
   float healthWithAbsorption,
   boolean targetHoldsTotem,
   float armor,
   float toughness,
   boolean hasResistance,
   float resistanceAmplifier,
   boolean assumeBestArmor,
   int protectionScore,
   boolean hasBlockingComponent,
   List<ExplosionBlockingDamageMath.Reduction> blockingReductions
) {
   public ExplosionDamageSnapshot {
      Objects.requireNonNull(targetUuid, "targetUuid");
      Objects.requireNonNull(dimensionId, "dimensionId");
      Objects.requireNonNull(difficulty, "difficulty");
      blockingReductions = List.copyOf(
         Objects.requireNonNull(blockingReductions, "blockingReductions")
      );
   }

   /**
    * Captures the mutable target-side inputs once on the client thread.
    * worldGeneration must be incremented by the caller whenever the client
    * world/session is replaced or invalidated.
    */
   public static ExplosionDamageSnapshot capture(
      ClientLevel level,
      LivingEntity target,
      AABB bounds,
      boolean assumeBestArmor,
      long worldGeneration
   ) {
      Objects.requireNonNull(level, "level");
      Objects.requireNonNull(target, "target");
      Objects.requireNonNull(bounds, "bounds");

      Minecraft minecraft = Minecraft.getInstance();
      if (!minecraft.isSameThread()) {
         throw new IllegalStateException("Explosion damage profiles must be captured on the client thread");
      }
      if (minecraft.level != level || target.level() != level) {
         throw new IllegalStateException("Explosion damage profile must belong to the active client world");
      }

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
         List<ItemStack> armorStacks = EquipmentUtils.getArmorStacks(player);
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

      double viewX = 0.0D;
      double viewZ = 0.0D;
      boolean hasBlockingComponent = false;
      List<ExplosionBlockingDamageMath.Reduction> blockingReductions = new ArrayList<>();
      if (target.isUsingItem()) {
         ItemStack usedItem = target.getUseItem();
         BlocksAttacks blocksAttacks = (BlocksAttacks)usedItem.get(DataComponents.BLOCKS_ATTACKS);
         if (blocksAttacks != null) {
            hasBlockingComponent = true;
            DamageSources damageSources = level.damageSources();
            DamageSource damageSource = damageSources.explosion((Explosion)null);
            Vec3 viewVector = target.calculateViewVector(0.0F, target.getYHeadRot());
            viewX = viewVector.x;
            viewZ = viewVector.z;

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

      return new ExplosionDamageSnapshot(
         target.getId(),
         target.getUUID(),
         worldGeneration,
         level.getGameTime(),
         level.dimension().identifier().toString(),
         bounds.minX,
         bounds.minY,
         bounds.minZ,
         bounds.maxX,
         bounds.maxY,
         bounds.maxZ,
         target.getX(),
         target.getZ(),
         viewX,
         viewZ,
         difficulty,
         immune,
         target.getHealth() + target.getAbsorptionAmount(),
         target.getMainHandItem().is(Items.TOTEM_OF_UNDYING)
            || target.getOffhandItem().is(Items.TOTEM_OF_UNDYING),
         armor,
         toughness,
         hasResistance,
         resistanceAmplifier,
         assumeBestArmor,
         protectionScore,
         hasBlockingComponent,
         blockingReductions
      );
   }

   /**
    * Calculates one explosion using only this profile, primitive explosion
    * coordinates, a per-candidate immutable query, and the matching ray
    * snapshot. Mismatched or incomplete ray data must be discarded.
    */
   public DamageResult calculate(
      double explosionX,
      double explosionY,
      double explosionZ,
      double power,
      ExplosionRaycastSnapshot.Query rayQuery,
      ExplosionRaycastSnapshot raySnapshot
   ) {
      Objects.requireNonNull(rayQuery, "rayQuery");
      Objects.requireNonNull(raySnapshot, "raySnapshot");
      if (raySnapshot.worldGeneration() != this.worldGeneration
         || raySnapshot.captureTick() != this.captureTick
         || !raySnapshot.dimensionId().equals(this.dimensionId)
         || !raySnapshot.matchesCollisionEntity(this.targetEntityId, this.targetUuid)) {
         return DamageResult.incomplete();
      }

      double bottomCenterX = this.minX + (this.maxX - this.minX) * 0.5D;
      double bottomCenterY = this.minY;
      double bottomCenterZ = this.minZ + (this.maxZ - this.minZ) * 0.5D;
      double normalizedDistance = new Vec3(bottomCenterX, bottomCenterY, bottomCenterZ)
         .distanceTo(new Vec3(explosionX, explosionY, explosionZ)) / (power * 2.0D);

      if (this.immune || normalizedDistance > 1.0D) {
         return new DamageResult(
            0.0F,
            0.0F,
            0,
            true,
            0.0F >= this.healthWithAbsorption,
            this.targetHoldsTotem
         );
      }

      ExplosionExposureMath.ExposureResult exposure = ExplosionExposureMath.calculateFromSnapshot(
         this.minX,
         this.minY,
         this.minZ,
         this.maxX,
         this.maxY,
         this.maxZ,
         explosionX,
         explosionY,
         explosionZ,
         raySnapshot,
         rayQuery
      );
      if (!exposure.complete()) {
         return DamageResult.incomplete();
      }

      float damage = ExplosionDamageMath.calculate(
         normalizedDistance,
         exposure.exposure(),
         power,
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
         Vec3 horizontalDirection = new Vec3(
            explosionX - this.targetX,
            0.0D,
            explosionZ - this.targetZ
         ).normalize();
         double angle = Math.acos(horizontalDirection.x * this.viewX + horizontalDirection.z * this.viewZ);
         damage = ExplosionBlockingDamageMath.apply(
            damage,
            angle,
            this.blockingReductions
         );
      }

      float finalDamage = Math.max(damage, 0.0F);
      return new DamageResult(
         finalDamage,
         exposure.exposure(),
         exposure.totalSamples(),
         true,
         finalDamage >= this.healthWithAbsorption,
         this.targetHoldsTotem
      );
   }

   public record DamageResult(
      float damage,
      float exposure,
      int exposureSamples,
      boolean complete,
      boolean lethal,
      boolean targetHoldsTotem
   ) {
      public static DamageResult incomplete() {
         return new DamageResult(0.0F, 0.0F, 0, false, false, false);
      }
   }
}
