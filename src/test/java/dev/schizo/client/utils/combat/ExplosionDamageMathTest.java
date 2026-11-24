package dev.schizo.client.utils.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ExplosionDamageMathTest {
   @Test
   void returnsZeroOutsideExplosionDiameter() {
      assertEquals(0.0F, ExplosionDamageMath.calculate(1.0001D, 1.0F, 6.0D,
         ExplosionDamageMath.Difficulty.NORMAL));
   }

   @Test
   void calculatesFullExposureAtExplosionCenter() {
      assertEquals(85.0F, ExplosionDamageMath.calculate(0.0D, 1.0F, 6.0D,
         ExplosionDamageMath.Difficulty.NORMAL));
   }

   @Test
   void zeroExposureRetainsVanillaBasePointBeforeDifficultyScaling() {
      assertEquals(1.0F, ExplosionDamageMath.calculate(0.5D, 0.0F, 5.0D,
         ExplosionDamageMath.Difficulty.NORMAL));
   }

   @Test
   void appliesEachDifficultyModifier() {
      double distance = 0.5D;
      float exposure = 0.75F;
      double power = 5.0D;
      float normal = ExplosionDamageMath.calculate(distance, exposure, power,
         ExplosionDamageMath.Difficulty.NORMAL);

      assertEquals(0.0F, ExplosionDamageMath.calculate(distance, exposure, power,
         ExplosionDamageMath.Difficulty.PEACEFUL));
      assertEquals(Math.min(normal / 2.0F + 1.0F, normal),
         ExplosionDamageMath.calculate(distance, exposure, power, ExplosionDamageMath.Difficulty.EASY));
      assertEquals(normal, ExplosionDamageMath.calculate(distance, exposure, power,
         ExplosionDamageMath.Difficulty.NORMAL));
      assertEquals(normal * 1.5F, ExplosionDamageMath.calculate(distance, exposure, power,
         ExplosionDamageMath.Difficulty.HARD));
   }

   @Test
   void matchesTheCapturedNormalDifficultyReferenceFixture() {
      assertEquals(19.046875F, ExplosionDamageMath.calculate(0.5D, 0.75F, 5.0D,
         ExplosionDamageMath.Difficulty.NORMAL));
   }

   @Test
   void preservesTheBoundaryAtNormalizedDistanceOne() {
      assertEquals(1.0F, ExplosionDamageMath.calculate(1.0D, 1.0F, 6.0D,
         ExplosionDamageMath.Difficulty.NORMAL));
   }
}
