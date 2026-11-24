package dev.schizo.client.utils.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ExplosionDamageReductionMathTest {
   @Test
   void appliesArmorUsingDamageSensitiveToughnessFormula() {
      assertEquals(8.0F, ExplosionDamageReductionMath.apply(
         20.0F, 20.0F, 8.0F, false, 1.0F, false, 0
      ), 0.00001F);
   }

   @Test
   void preservesVanillaClampOrderingWhenTheArmorBoundsCross() {
      assertEquals(4.0F, ExplosionDamageReductionMath.apply(
         20.0F, 101.0F, 0.0F, false, 1.0F, false, 0
      ), 0.00001F);
   }

   @Test
   void appliesResistanceAfterArmor() {
      assertEquals(6.4F, ExplosionDamageReductionMath.apply(
         20.0F, 20.0F, 8.0F, true, 0.0F, false, 0
      ), 0.00001F);
   }

   @Test
   void appliesProtectionAfterResistanceAndCapsProtectionAtTwenty() {
      float expected = 4.352F;

      assertEquals(expected, ExplosionDamageReductionMath.apply(
         20.0F, 20.0F, 8.0F, true, 0.0F, false, 8
      ), 0.00001F);
      assertEquals(1.28F, ExplosionDamageReductionMath.apply(
         20.0F, 20.0F, 8.0F, true, 0.0F, false, 30
      ), 0.00001F);
   }

   @Test
   void preservesAssumeBestArmorResistanceFallbackWhenEffectIsAbsent() {
      assertEquals(6.0F, ExplosionDamageReductionMath.apply(
         10.0F, 0.0F, 0.0F, false, 1.0F, true, 0
      ));
   }

   @Test
   void clampsNegativeFinalDamageToZero() {
      assertEquals(0.0F, ExplosionDamageReductionMath.apply(
         10.0F, 0.0F, 0.0F, true, 5.0F, false, 0
      ));
   }

   @Test
   void preservesTheUpperProtectionClamp() {
      assertEquals(1.6F, ExplosionDamageReductionMath.apply(
         20.0F, 20.0F, 8.0F, false, 1.0F, false, 20
      ), 0.00001F);
   }
}
