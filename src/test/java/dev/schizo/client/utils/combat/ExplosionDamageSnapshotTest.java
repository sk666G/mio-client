package dev.schizo.client.utils.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class ExplosionDamageSnapshotTest {
   private static final UUID TARGET_UUID = UUID.fromString("d5d7a90b-2541-45a4-9bc0-c7c8ca20e1f0");

   @Test
   void calculatesDamageFromOnlyCapturedValuesAndMatchingRaySnapshot() {
      ExplosionDamageSnapshot damage = snapshot(false, 0.5D);
      ExplosionRaycastSnapshot rays = raySnapshot(
         42,
         100L,
         12L,
         "minecraft:overworld",
         airBlocks()
      );

      ExplosionDamageSnapshot.DamageResult result = damage.calculate(rays);

      assertTrue(result.complete());
      assertEquals(1.0F, result.exposure());
      assertEquals(32.5F, result.damage(), 0.00001F);
      assertEquals(8, result.exposureSamples());
   }

   @Test
   void rejectsRaySnapshotsFromAnotherWorldGeneration() {
      ExplosionDamageSnapshot damage = snapshot(false, 0.5D);
      ExplosionRaycastSnapshot rays = raySnapshot(
         42,
         101L,
         12L,
         "minecraft:overworld",
         airBlocks()
      );

      assertFalse(damage.calculate(rays).complete());
   }

   @Test
   void rejectsRaySnapshotsFromAnotherCaptureTick() {
      ExplosionDamageSnapshot damage = snapshot(false, 0.5D);
      ExplosionRaycastSnapshot rays = raySnapshot(
         42,
         100L,
         13L,
         "minecraft:overworld",
         airBlocks()
      );

      assertFalse(damage.calculate(rays).complete());
   }

   @Test
   void rejectsRaySnapshotsFromAnotherDimension() {
      ExplosionDamageSnapshot damage = snapshot(false, 0.5D);
      ExplosionRaycastSnapshot rays = raySnapshot(
         42,
         100L,
         12L,
         "minecraft:the_nether",
         airBlocks()
      );

      assertFalse(damage.calculate(rays).complete());
   }

   @Test
   void rejectsRaySnapshotsCapturedForAnotherEntityCollisionProfile() {
      ExplosionDamageSnapshot damage = snapshot(false, 0.5D);
      ExplosionRaycastSnapshot rays = raySnapshot(
         43,
         100L,
         12L,
         "minecraft:overworld",
         airBlocks()
      );

      assertFalse(damage.calculate(rays).complete());
   }

   @Test
   void rejectsAReusedEntityIdWithDifferentUuid() {
      ExplosionDamageSnapshot damage = snapshot(false, 0.5D);
      ExplosionRaycastSnapshot rays = new ExplosionRaycastSnapshot(
         100L,
         12L,
         "minecraft:overworld",
         42,
         UUID.fromString("b8caa20e-1d88-4d42-a608-b7b63df26149"),
         airBlocks()
      );

      assertFalse(damage.calculate(rays).complete());
   }

   @Test
   void appliesCapturedArmorResistanceAndProtectionInOrder() {
      ExplosionDamageSnapshot damage = new ExplosionDamageSnapshot(
         42, TARGET_UUID, 100L, 12L, "minecraft:overworld",
         0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D,
         0.5D, 0.5D, 0.5D, 0.0D, 6.0D,
         ExplosionDamageMath.Difficulty.NORMAL,
         false, 20.0F, 20.0F, 8.0F, true, 0.0F, false, 5,
         false, 0.0D, List.of(), ExplosionRaycastSnapshot.Query.normal()
      );

      ExplosionDamageSnapshot.DamageResult result = damage.calculate(
         raySnapshot(42, 100L, 12L, "minecraft:overworld", airBlocks())
      );

      assertTrue(result.complete());
      assertEquals(45.696F, result.damage(), 0.0001F);
   }

   @Test
   void appliesCapturedBlockingRulesAfterOtherDamageReduction() {
      ExplosionDamageSnapshot damage = new ExplosionDamageSnapshot(
         42, TARGET_UUID, 100L, 12L, "minecraft:overworld",
         0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D,
         0.5D, 0.5D, 0.5D, 0.0D, 6.0D,
         ExplosionDamageMath.Difficulty.NORMAL,
         false, 20.0F, 0.0F, 0.0F, false, 1.0F, false, 0,
         true, 0.0D,
         List.of(new ExplosionBlockingDamageMath.Reduction(90.0F, true, 1.0F, 0.5F)),
         ExplosionRaycastSnapshot.Query.normal()
      );

      ExplosionDamageSnapshot.DamageResult result = damage.calculate(
         raySnapshot(42, 100L, 12L, "minecraft:overworld", airBlocks())
      );

      assertTrue(result.complete());
      assertEquals(43.5F, result.damage(), 0.0001F);
   }

   @Test
   void mismatchedCapturedBlockingDamageTypeCanReduceDamageToZero() {
      ExplosionDamageSnapshot damage = new ExplosionDamageSnapshot(
         42, TARGET_UUID, 100L, 12L, "minecraft:overworld",
         0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D,
         0.5D, 0.5D, 0.5D, 0.0D, 6.0D,
         ExplosionDamageMath.Difficulty.NORMAL,
         false, 0.0F, 0.0F, false, 1.0F, false, 0,
         true, 0.0D,
         List.of(new ExplosionBlockingDamageMath.Reduction(180.0F, false, 0.0F, 1.0F)),
         ExplosionRaycastSnapshot.Query.normal()
      );

      ExplosionDamageSnapshot.DamageResult result = damage.calculate(
         raySnapshot(42, 100L, 12L, "minecraft:overworld", airBlocks())
      );

      assertTrue(result.complete());
      assertEquals(0.0F, result.damage());
   }

   @Test
   void returnsZeroForImmuneAndOutOfRangeTargetsWithoutRayCoverage() {
      ExplosionDamageSnapshot immune = snapshot(true, 0.5D);
      ExplosionDamageSnapshot outOfRange = snapshot(false, 2.0D);
      ExplosionRaycastSnapshot emptyRays = raySnapshot(
         42,
         100L,
         12L,
         "minecraft:overworld",
         Map.of()
      );

      assertEquals(0.0F, immune.calculate(emptyRays).damage());
      assertTrue(immune.calculate(emptyRays).complete());
      assertEquals(0.0F, outOfRange.calculate(emptyRays).damage());
      assertTrue(outOfRange.calculate(emptyRays).complete());
   }

   @Test
   void propagatesIncompleteRayCoverageInsteadOfGuessingDamage() {
      ExplosionDamageSnapshot damage = snapshot(false, 0.5D);
      ExplosionRaycastSnapshot incompleteRays = raySnapshot(
         42,
         100L,
         12L,
         "minecraft:overworld",
         Map.of()
      );

      ExplosionDamageSnapshot.DamageResult result = damage.calculate(incompleteRays);

      assertFalse(result.complete());
      assertEquals(0.0F, result.damage());
   }

   @Test
   void defensivelyCopiesBlockingRules() {
      var reductions = new java.util.ArrayList<ExplosionBlockingDamageMath.Reduction>();
      reductions.add(new ExplosionBlockingDamageMath.Reduction(90.0F, true, 0.0F, 1.0F));
      ExplosionDamageSnapshot value = new ExplosionDamageSnapshot(
         42, TARGET_UUID, 100L, 12L, "minecraft:overworld",
         0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D,
         0.5D, 0.5D, 0.5D, 0.5D, 6.0D,
         ExplosionDamageMath.Difficulty.NORMAL,
         false, 20.0F, 0.0F, 0.0F, false, 1.0F, false, 0,
         true, 0.0D, reductions, ExplosionRaycastSnapshot.Query.normal()
      );
      reductions.clear();

      assertEquals(1, value.blockingReductions().size());
   }

   private static ExplosionDamageSnapshot snapshot(boolean immune, double normalizedDistance) {
      return new ExplosionDamageSnapshot(
         42, TARGET_UUID, 100L, 12L, "minecraft:overworld",
         0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D,
         0.5D, 0.5D, 0.5D, normalizedDistance, 6.0D,
         ExplosionDamageMath.Difficulty.NORMAL,
         immune, 20.0F, 0.0F, 0.0F, false, 1.0F, false, 0,
         false, 0.0D, List.of(), ExplosionRaycastSnapshot.Query.normal()
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
      return Map.of(
         new BlockPos(-1, -1, -1).asLong(), air(),
         new BlockPos(-1, -1, 0).asLong(), air(),
         new BlockPos(-1, 0, 0).asLong(), air(),
         BlockPos.ZERO.asLong(), air()
      );
   }

   private static ExplosionRaycastSnapshot.BlockGeometry air() {
      return new ExplosionRaycastSnapshot.BlockGeometry(0.0F, List.of());
   }
}
