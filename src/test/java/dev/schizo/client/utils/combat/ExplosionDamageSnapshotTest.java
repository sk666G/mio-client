package dev.schizo.client.utils.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;\nimport static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class ExplosionDamageSnapshotTest {
   private static final UUID TARGET_UUID = UUID.fromString("d5d7a90b-2541-45a4-9bc0-c7c8ca20e1f0");

   @Test
   void calculatesDamageFromCapturedTargetProfileAndMatchingRaySnapshot() {
      ExplosionDamageSnapshot profile = profile(false, 100.0F, false);
      ExplosionDamageSnapshot.DamageResult result = calculate(
         profile,
         0.5D,
         raySnapshot(42, 100L, 12L, "minecraft:overworld", airBlocks())
      );

      assertTrue(result.complete());
      assertEquals(1.0F, result.exposure());
      assertEquals(3.625F, result.damage(), 0.00001F);
      assertEquals(8, result.exposureSamples());
      assertFalse(result.lethal());
      assertFalse(result.targetHoldsTotem());
   }

   @Test
   void reusesOneTargetProfileAcrossDifferentExplosionPositions() {
      ExplosionDamageSnapshot profile = profile(false, 100.0F, false);
      ExplosionRaycastSnapshot rays = raySnapshot(42, 100L, 12L, "minecraft:overworld", rayCells());

      ExplosionDamageSnapshot.DamageResult near = profile.calculate(
         0.5D, 0.0D, 0.0D, 0.5D, ExplosionRaycastSnapshot.Query.normal(), rays
      );
      ExplosionDamageSnapshot.DamageResult farther = profile.calculate(
         0.5D, 0.5D, 0.5D, 0.5D, ExplosionRaycastSnapshot.Query.normal(), rays
      );

      assertTrue(near.complete());
      assertTrue(farther.complete());
      assertTrue(near.damage() > farther.damage());
   }

   @Test
   void rejectsInvalidCandidateExplosionInputs() {
      ExplosionDamageSnapshot profile = profile(false, 100.0F, false);
      ExplosionRaycastSnapshot rays = raySnapshot(42, 100L, 12L, "minecraft:overworld", airBlocks());

      assertThrows(IllegalArgumentException.class, () ->
         profile.calculate(0.5D, 0.5D, 0.5D, 0.0D, ExplosionRaycastSnapshot.Query.normal(), rays));
      assertThrows(IllegalArgumentException.class, () ->
         profile.calculate(0.5D, 0.5D, 0.5D, -1.0D, ExplosionRaycastSnapshot.Query.normal(), rays));
      assertThrows(IllegalArgumentException.class, () ->
         profile.calculate(Double.NaN, 0.5D, 0.5D, 0.5D, ExplosionRaycastSnapshot.Query.normal(), rays));
      assertThrows(IllegalArgumentException.class, () ->
         profile.calculate(0.5D, Double.POSITIVE_INFINITY, 0.5D, 0.5D, ExplosionRaycastSnapshot.Query.normal(), rays));
   }

   @Test
   void rejectsRaySnapshotsFromAnotherWorldGeneration() {
      ExplosionDamageSnapshot profile = profile(false, 100.0F, false);
      assertFalse(calculate(
         profile,
         0.5D,
         raySnapshot(42, 101L, 12L, "minecraft:overworld", airBlocks())
      ).complete());
   }

   @Test
   void rejectsRaySnapshotsFromAnotherCaptureTick() {
      ExplosionDamageSnapshot profile = profile(false, 100.0F, false);
      assertFalse(calculate(
         profile,
         0.5D,
         raySnapshot(42, 100L, 13L, "minecraft:overworld", airBlocks())
      ).complete());
   }

   @Test
   void rejectsRaySnapshotsFromAnotherDimension() {
      ExplosionDamageSnapshot profile = profile(false, 100.0F, false);
      assertFalse(calculate(
         profile,
         0.5D,
         raySnapshot(42, 100L, 12L, "minecraft:the_nether", airBlocks())
      ).complete());
   }

   @Test
   void rejectsRaySnapshotsCapturedForAnotherEntityCollisionProfile() {
      ExplosionDamageSnapshot profile = profile(false, 100.0F, false);
      assertFalse(calculate(
         profile,
         0.5D,
         raySnapshot(43, 100L, 12L, "minecraft:overworld", airBlocks())
      ).complete());
   }

   @Test
   void rejectsAReusedEntityIdWithDifferentUuid() {
      ExplosionDamageSnapshot profile = profile(false, 100.0F, false);
      ExplosionRaycastSnapshot rays = new ExplosionRaycastSnapshot(
         100L,
         12L,
         "minecraft:overworld",
         42,
         UUID.fromString("b8caa20e-1d88-4d42-a608-b7b63df26149"),
         airBlocks()
      );

      assertFalse(calculate(profile, 0.5D, rays).complete());
   }

   @Test
   void appliesCapturedArmorResistanceAndProtectionInOrder() {
      ExplosionDamageSnapshot profile = profile(
         false, 100.0F, false,
         20.0F, 8.0F, true, 0.0F, 5
      );

      ExplosionDamageSnapshot.DamageResult result = calculate(
         profile, 0.5D,
         raySnapshot(42, 100L, 12L, "minecraft:overworld", airBlocks())
      );

      assertTrue(result.complete());
      assertEquals(0.5481F, result.damage(), 0.0001F);
   }

   @Test
   void appliesCapturedBlockingRulesAfterOtherDamageReduction() {
      ExplosionDamageSnapshot profile = profile(
         false, 100.0F, false,
         0.0F, 0.0F, false, 1.0F, 0,
         true,
         List.of(new ExplosionBlockingDamageMath.Reduction(90.0F, true, 1.0F, 0.5F)),
         0.0D, 0.0D, 0.0D, 1.0D
      );

      ExplosionDamageSnapshot.DamageResult result = calculate(
         profile, 0.5D,
         raySnapshot(42, 100L, 12L, "minecraft:overworld", airBlocks())
      );

      assertTrue(result.complete());
      assertEquals(2.8125F, result.damage(), 0.0001F);
   }

   @Test
   void mismatchedCapturedBlockingDamageTypeCanReduceDamageToZero() {
      ExplosionDamageSnapshot profile = profile(
         false, 100.0F, false,
         0.0F, 0.0F, false, 1.0F, 0,
         true,
         List.of(new ExplosionBlockingDamageMath.Reduction(180.0F, false, 0.0F, 1.0F)),
         0.0D, 0.0D, 0.0D, 1.0D
      );

      ExplosionDamageSnapshot.DamageResult result = calculate(
         profile, 0.5D,
         raySnapshot(42, 100L, 12L, "minecraft:overworld", airBlocks())
      );

      assertTrue(result.complete());
      assertEquals(0.0F, result.damage());
   }

   @Test
   void capturesLethalAndTotemFlagsForLaterTargetPolicy() {
      ExplosionDamageSnapshot profile = profile(true, 1.0F, true);
      ExplosionDamageSnapshot.DamageResult result = calculate(
         profile, 0.5D,
         raySnapshot(42, 100L, 12L, "minecraft:overworld", airBlocks())
      );

      assertTrue(result.complete());
      assertTrue(result.lethal());
      assertTrue(result.targetHoldsTotem());
   }

   @Test
   void returnsZeroForImmuneAndOutOfRangeTargetsWithoutRayCoverage() {
      ExplosionDamageSnapshot immune = profile(true, 20.0F, false);
      ExplosionDamageSnapshot regular = profile(false, 20.0F, false);
      ExplosionRaycastSnapshot emptyRays = raySnapshot(
         42, 100L, 12L, "minecraft:overworld", Map.of()
      );

      assertEquals(0.0F, calculate(immune, 0.5D, emptyRays).damage());
      assertTrue(calculate(immune, 0.5D, emptyRays).complete());
      assertEquals(0.0F, calculate(regular, 0.2D, emptyRays).damage());
      assertTrue(calculate(regular, 0.2D, emptyRays).complete());
   }

   @Test
   void propagatesIncompleteRayCoverageInsteadOfGuessingDamage() {
      ExplosionDamageSnapshot profile = profile(false, 20.0F, false);
      ExplosionDamageSnapshot.DamageResult result = calculate(
         profile,
         0.5D,
         raySnapshot(42, 100L, 12L, "minecraft:overworld", Map.of())
      );

      assertFalse(result.complete());
      assertEquals(0.0F, result.damage());
      assertFalse(result.lethal());
   }

   @Test
   void defensivelyCopiesBlockingRules() {
      var reductions = new java.util.ArrayList<ExplosionBlockingDamageMath.Reduction>();
      reductions.add(new ExplosionBlockingDamageMath.Reduction(90.0F, true, 0.0F, 1.0F));
      ExplosionDamageSnapshot value = new ExplosionDamageSnapshot(
         42, TARGET_UUID, 100L, 12L, "minecraft:overworld",
         0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D,
         0.0D, 0.0D, 0.0D, 1.0D,
         ExplosionDamageMath.Difficulty.NORMAL,
         false, 20.0F, false,
         0.0F, 0.0F, false, 1.0F, false, 0,
         true, reductions
      );
      reductions.clear();

      assertEquals(1, value.blockingReductions().size());
   }

   private static ExplosionDamageSnapshot profile(
      boolean immune,
      float health,
      boolean holdingTotem
   ) {
      return profile(
         immune, health, holdingTotem,
         0.0F, 0.0F, false, 1.0F, 0
      );
   }

   private static ExplosionDamageSnapshot profile(
      boolean immune,
      float health,
      boolean holdingTotem,
      float armor,
      float toughness,
      boolean hasResistance,
      float resistanceAmplifier,
      int protectionScore
   ) {
      return profile(
         immune, health, holdingTotem,
         armor, toughness, hasResistance, resistanceAmplifier, protectionScore,
         false, List.of(), 0.0D, 0.0D, 0.0D, 1.0D
      );
   }

   private static ExplosionDamageSnapshot profile(
      boolean immune,
      float health,
      boolean holdingTotem,
      float armor,
      float toughness,
      boolean hasResistance,
      float resistanceAmplifier,
      int protectionScore,
      boolean hasBlocking,
      List<ExplosionBlockingDamageMath.Reduction> blockingReductions,
      double targetX,
      double targetZ,
      double viewX,
      double viewZ
   ) {
      return new ExplosionDamageSnapshot(
         42, TARGET_UUID, 100L, 12L, "minecraft:overworld",
         0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D,
         targetX, targetZ, viewX, viewZ,
         ExplosionDamageMath.Difficulty.NORMAL,
         immune, health, holdingTotem,
         armor, toughness, hasResistance, resistanceAmplifier, false, protectionScore,
         hasBlocking, blockingReductions
      );
   }

   private static ExplosionDamageSnapshot.DamageResult calculate(
      ExplosionDamageSnapshot profile,
      double power,
      ExplosionRaycastSnapshot rays
   ) {
      return profile.calculate(
         0.5D,
         0.0D,
         0.0D,
         power,
         ExplosionRaycastSnapshot.Query.normal(),
         rays
      );
   }

   private static ExplosionRaycastSnapshot raySnapshot(
      int entityId,
      long worldGeneration,
      long tick,
      String dimension,
      Map<Long, ExplosionRaycastSnapshot.BlockGeometry> blocks
   ) {
      return new ExplosionRaycastSnapshot(
         worldGeneration, tick, dimension, entityId, TARGET_UUID, blocks
      );
   }

   private static Map<Long, ExplosionRaycastSnapshot.BlockGeometry> airBlocks() {
      return rayCells();
   }

   private static Map<Long, ExplosionRaycastSnapshot.BlockGeometry> rayCells() {
      return Map.of(
         new BlockPos(-1, -1, -1).asLong(), air(),
         new BlockPos(0, -1, -1).asLong(), air(),
         new BlockPos(-1, -1, 0).asLong(), air(),
         new BlockPos(-1, 0, 0).asLong(), air(),
         BlockPos.ZERO.asLong(), air()
      );
   }

   private static ExplosionRaycastSnapshot.BlockGeometry air() {
      return new ExplosionRaycastSnapshot.BlockGeometry(0.0F, List.of());
   }
}
