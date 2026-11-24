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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
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
 * Client-thread adapter that converts one live target into a data-only
 * ExplosionDamageProfile.
 */
public final class ExplosionDamageProfileBuilder {
   private ExplosionDamageProfileBuilder() {
   }

   public static ExplosionDamageProfile capture(
      LivingEntity target,
      ClientLevel level,
      boolean assumeBestArmor,
      long worldGeneration,
      long captureTick
   ) {
      Objects.requireNonNull(target, "target");
      Objects.requireNonNull(level, "level");

      Minecraft minecraft = Minecraft.getInstance();
      if (!minecraft.isSameThread()) {
         throw new IllegalStateException("Explosion damage profiles must be captured on the client thread");
      }
      if (minecraft.level != level || target.level() != level) {
         throw new IllegalStateException("Cannot capture a damage profile for an inactive or different world");
      }

      AABB bounds = target.getBoundingBox();
      Vec3 position = target.position();
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

      int protectionScore = captureProtectionScore(target, assumeBestArmor);
      boolean hasBlockingItem = false;
      List<ExplosionBlockingDamageMath.Reduction> blockingReductions = new ArrayList<>();
      double viewX = 0.0D;
      double viewZ = 0.0D;
      if (target.isUsingItem()) {
         ItemStack usedItem = target.getUseItem();
         BlocksAttacks blocksAttacks = usedItem.get(DataComponents.BLOCKS_ATTACKS);
         if (blocksAttacks != null) {
            hasBlockingItem = true;
            DamageSources damageSources = level.damageSources();
            DamageSource explosionDamageSource = damageSources.explosion((Explosion)null);
            for (BlocksAttacks.DamageReduction reduction : blocksAttacks.damageReductions()) {
               boolean damageTypeMatches = reduction.type().isEmpty()
                  || reduction.type().get().contains(explosionDamageSource.typeHolder());
               blockingReductions.add(new ExplosionBlockingDamageMath.Reduction(
                  reduction.horizontalBlockingAngle(),
                  damageTypeMatches,
                  reduction.base(),
                  reduction.factor()
               ));
            }

            Vec3 viewVector = target.calculateViewVector(0.0F, target.getYHeadRot());
            viewX = viewVector.x;
            viewZ = viewVector.z;
         }
      }

      LocalPlayer localPlayer = minecraft.player;
      boolean creativeSelfDamageImmune = target == localPlayer && localPlayer != null && localPlayer.isCreative();
      ExplosionDamageMath.Difficulty difficulty = switch (level.getDifficulty()) {
         case PEACEFUL -> ExplosionDamageMath.Difficulty.PEACEFUL;
         case EASY -> ExplosionDamageMath.Difficulty.EASY;
         case HARD -> ExplosionDamageMath.Difficulty.HARD;
         default -> ExplosionDamageMath.Difficulty.NORMAL;
      };

      return new ExplosionDamageProfile(
         target.getId(),
         target.getUUID().toString(),
         worldGeneration,
         captureTick,
         level.dimension().identifier().toString(),
         creativeSelfDamageImmune,
         target.getHealth() + target.getAbsorptionAmount(),
         bounds.minX,
         bounds.minY,
         bounds.minZ,
         bounds.maxX,
         bounds.maxY,
         bounds.maxZ,
         position.x,
         position.z,
         viewX,
         viewZ,
         armor,
         toughness,
         hasResistance,
         resistanceAmplifier,
         assumeBestArmor,
         protectionScore,
         hasBlockingItem,
         blockingReductions,
         difficulty
      );
   }

   private static int captureProtectionScore(LivingEntity target, boolean assumeBestArmor) {
      if (!(target instanceof Player player)) {
         return 0;
      }

      int protectionScore = 0;
      int assumedArmorUnits = 1;
      List<ItemStack> armorStacks = EquipmentUtils.getArmorStacks(player);
      Iterator<ItemStack> armorItems = armorStacks.iterator();
      while (armorItems.hasNext()) {
         ItemStack armorItem = armorItems.next();

         if (!armorItem.isEmpty()) {
            TagKey headArmorTag = ItemTags.HEAD_ARMOR;
            if (armorItem.is(headArmorTag)) {
               assumedArmorUnits--;
            }

            Map enchantments = EnchantmentUtils.getLevels(armorItem);
            ResourceKey blastProtectionKey = Enchantments.BLAST_PROTECTION;
            int blastContribution = (Integer)enchantments.getOrDefault(blastProtectionKey, 0);
            blastContribution *= 2;
            ResourceKey protectionKey = Enchantments.PROTECTION;
            int protectionLevel = (Integer)enchantments.getOrDefault(protectionKey, 0);
            protectionScore += blastContribution + protectionLevel;
            assumedArmorUnits++;
         }
      }

      if (assumeBestArmor) {
         float assumedArmorFraction = (float)assumedArmorUnits / 4.0F;
         protectionScore = (int)(assumedArmorFraction * 20.0F);
      }

      return Math.min(protectionScore, 20);
   }
}
