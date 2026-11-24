package dev.schizo.client.utils.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ExplosionBlockingDamageMathTest {
   @Test
   void appliesMatchingReductionInsideItsAngle() {
      assertEquals(7.0F, ExplosionBlockingDamageMath.apply(
         10.0F,
         0.0D,
         List.of(new ExplosionBlockingDamageMath.Reduction(90.0F, true, 2.0F, 0.5F))
      ));
   }

   @Test
   void ignoresReductionOutsideItsAngle() {
      assertEquals(0.0F, ExplosionBlockingDamageMath.apply(
         10.0F,
         Math.PI,
         List.of(new ExplosionBlockingDamageMath.Reduction(90.0F, true, 0.0F, 1.0F))
      ));
   }

   @Test
   void ignoresDamageTypeMismatch() {
      assertEquals(0.0F, ExplosionBlockingDamageMath.apply(
         10.0F,
         0.0D,
         List.of(new ExplosionBlockingDamageMath.Reduction(180.0F, false, 0.0F, 1.0F))
      ));
   }

   @Test
   void sumsReductionRulesAndCapsTotalAtIncomingDamage() {
      List<ExplosionBlockingDamageMath.Reduction> rules = List.of(
         new ExplosionBlockingDamageMath.Reduction(180.0F, true, 0.0F, 0.75F),
         new ExplosionBlockingDamageMath.Reduction(180.0F, true, 0.0F, 0.75F)
      );

      assertEquals(10.0F, ExplosionBlockingDamageMath.apply(10.0F, 0.0D, rules));
   }

   @Test
   void clampsNegativeRuleContributionsToZero() {
      assertEquals(0.0F, ExplosionBlockingDamageMath.apply(
         10.0F,
         0.0D,
         List.of(new ExplosionBlockingDamageMath.Reduction(180.0F, true, -5.0F, 0.0F))
      ));
   }
}
