package dev.schizo.client.utils.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

class ExplosionDamageCalculatorTest {
   private static final double EXPLOSION_X = 0.5D;
   private static final double EXPLOSION_Y = 0.5D;
   private static final double EXPLOSION_Z = 0.5D;
   private static final double POWER = 6.0D;

   @Test
   void calculatesDamageUsingOnlyProfileAndCapturedRayGeometry() {
      ExplosionDamageProfile profile = profile(false);
      ExplosionRaycastSnapshot snapshot = completeAirSnapshot();

      ExplosionDamageCalculator.Result result = ExplosionDamageCalculator.calculate(
         profile,
         snapshot,
         ExplosionRaycastSnapshot.Query.normal(),
         EXPLOSION_X,
         EXPLOSION_Y,
         EXPLOSION_Z,
         POWER
      );

      double distance = Math.sqrt(0.75D);
      double normalizedDistance = distance / (POWER * 2.0D);
      float expectedDamage = ExplosionDamageMath.calculate(
         normalizedDistance,
         1.0F,
         POWER,
         ExplosionDamageMath.Difficulty.NORMAL
      );
      assertEquals(ExplosionDamageCalculator.Status.COMPLETE, result.status());
      assertEquals(profile.targetEntityId(), result.targetEntityId());
      assertEquals(profile.targetUuid(), result.targetUuid());
      assertEquals(1.0F, result.exposure());
      assertEquals(expectedDamage, result.damage(), 0.00001F);
   }

   @Test
   void reportsIncompleteRatherThanGuessingWhenRayCellsAreMissing() {
      ExplosionDamageProfile profile = profile(false);
      ExplosionRaycastSnapshot incomplete = new ExplosionRaycastSnapshot(
         7L,
         12L,
         "minecraft:overworld",
         Map.of()
      );

      ExplosionDamageCalculator.Result result = ExplosionDamageCalculator.calculate(
         profile,
         incomplete,
         ExplosionRaycastSnapshot.Query.normal(),
         EXPLOSION_X,
         EXPLOSION_Y,
         EXPLOSION_Z,
         POWER
      );

      assertEquals(ExplosionDamageCalculator.Status.INCOMPLETE_SNAPSHOT, result.status());
      assertFalse(result.complete());
   }

   @Test
   void rejectsWorldGenerationTickAndDimensionMismatches() {
      ExplosionDamageProfile profile = profile(false);

      assertStale(profile, new ExplosionRaycastSnapshot(8L, 12L, "minecraft:overworld", Map.of()));
      assertStale(profile, new ExplosionRaycastSnapshot(7L, 13L, "minecraft:overworld", Map.of()));
      assertStale(profile, new ExplosionRaycastSnapshot(7L, 12L, "minecraft:the_nether", Map.of()));
   }

   @Test
   void creativeSelfDamageImmunityDoesNotNeedRaySamples() {
      ExplosionDamageProfile profile = profile(true);
      ExplosionRaycastSnapshot empty = new ExplosionRaycastSnapshot(
         7L, 12L, "minecraft:overworld", Map.of()
      );

      ExplosionDamageCalculator.Result result = ExplosionDamageCalculator.calculate(
         profile,
         empty,
         ExplosionRaycastSnapshot.Query.normal(),
         EXPLOSION_X,
         EXPLOSION_Y,
         EXPLOSION_Z,
         POWER
      );

      assertTrue(result.complete());
      assertEquals(0.0F, result.damage());
      assertEquals(0.0F, result.exposure());
   }

   @Test
   void appliesCapturedBlockingDamageInputs() {
      ExplosionDamageProfile original = profile(false);
      ExplosionDamageProfile blocking = new ExplosionDamageProfile(
         original.targetEntityId(), original.targetUuid(), original.worldGeneration(),
         original.captureTick(), original.dimensionId(), original.creativeSelfDamageImmune(),
         original.healthWithAbsorption(), original.minX(), original.minY(), original.minZ(),
         original.maxX(), original.maxY(), original.maxZ(), original.targetX(), original.targetZ(),
         0.0D, 1.0D, original.armor(), original.toughness(), original.hasResistance(),
         original.resistanceAmplifier(), original.assumeBestArmor(), original.protectionScore(),
         true,
         List.of(new ExplosionBlockingDamageMath.Reduction(180.0F, true, 0.0F, 0.5F)),
         original.difficulty()
      );

      ExplosionDamageCalculator.Result result = ExplosionDamageCalculator.calculate(
         blocking,
         completeAirSnapshot(),
         ExplosionRaycastSnapshot.Query.normal(),
         EXPLOSION_X,
         EXPLOSION_Y,
         EXPLOSION_Z,
         POWER
      );

      assertEquals(ExplosionDamageCalculator.Status.COMPLETE, result.status());
      assertTrue(result.damage() >= 0.0F);
      assertTrue(result.damage() <= 100.0F);
   }

   private static void assertStale(
      ExplosionDamageProfile profile,
      ExplosionRaycastSnapshot snapshot
   ) {
      ExplosionDamageCalculator.Result result = ExplosionDamageCalculator.calculate(
         profile,
         snapshot,
         ExplosionRaycastSnapshot.Query.normal(),
         EXPLOSION_X,
         EXPLOSION_Y,
         EXPLOSION_Z,
         POWER
      );
      assertEquals(ExplosionDamageCalculator.Status.STALE_SNAPSHOT, result.status());
      assertFalse(result.complete());
   }

   private static ExplosionDamageProfile profile(boolean creativeSelfDamageImmune) {
      ExplosionDamageProfile base = ExplosionDamageProfileTest.profile(List.of());
      return new ExplosionDamageProfile(
         base.targetEntityId(),
         base.targetUuid(),
         base.worldGeneration(),
         base.captureTick(),
         base.dimensionId(),
         creativeSelfDamageImmune,
         base.healthWithAbsorption(),
         base.minX(), base.minY(), base.minZ(),
         base.maxX(), base.maxY(), base.maxZ(),
         base.targetX(), base.targetZ(),
         base.viewX(), base.viewZ(),
         base.armor(), base.toughness(),
         base.hasResistance(), base.resistanceAmplifier(),
         base.assumeBestArmor(), base.protectionScore(),
         base.hasBlockingItem(), base.blockingReductions(),
         base.difficulty()
      );
   }

   private static ExplosionRaycastSnapshot completeAirSnapshot() {
      Map<Long, ExplosionRaycastSnapshot.BlockGeometry> blocks = new HashMap<>();
      for (int x = -1; x <= 0; x++) {
         for (int y = -1; y <= 0; y++) {
            for (int z = -1; z <= 0; z++) {
               blocks.put(
                  new BlockPos(x, y, z).asLong(),
                  new ExplosionRaycastSnapshot.BlockGeometry(0.0F, List.of())
               );
            }
         }
      }
      return new ExplosionRaycastSnapshot(7L, 12L, "minecraft:overworld", blocks);
   }
}
