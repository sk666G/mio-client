package dev.schizo.client.modules.combat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CrystalDamageSafetyInputsTest {
   @Test
   void forceSuicideOverridesAllOtherRejections() {
      assertFalse(inputs(false, false, true, true, false, false, false,
         10.0D, 20.0D, 8.0D, 4.0D, 1.5D).shouldReject(20.0D, 1.0D));
   }

   @Test
   void antiSuicideRejectsLethalSelfDamageUnlessTargetIsKilledWithTotemProtection() {
      assertTrue(inputs(false, false, false, true, false, false, false,
         10.0D, 20.0D, 8.0D, 30.0D, 0.0D).shouldReject(10.0D, 20.0D));

      assertFalse(inputs(false, false, false, true, true, true, false,
         10.0D, 20.0D, 8.0D, 30.0D, 0.0D).shouldReject(10.0D, 20.0D));
   }

   @Test
   void selfInvulnerabilityZerosSelfDamageBeforeSafetyChecks() {
      assertFalse(inputs(false, true, false, true, false, false, false,
         10.0D, 20.0D, 8.0D, 0.0D, 2.0D).shouldReject(100.0D, 10.0D));
   }

   @Test
   void hitIntersectionAndKillHaveTheirExistingEarlyAcceptPriority() {
      assertFalse(inputs(true, false, false, false, false, false, true,
         10.0D, 20.0D, 100.0D, 0.0D, 0.0D).shouldReject(100.0D, 1.0D));
      assertFalse(inputs(false, false, false, false, false, true, false,
         10.0D, 20.0D, 100.0D, 0.0D, 100.0D).shouldReject(100.0D, 1.0D));
   }

   @Test
   void placeRatioRejectsBeforeTargetHealthAndMaximumSelfDamage() {
      assertTrue(inputs(false, false, false, false, false, false, false,
         20.0D, 20.0D, 1.0D, 20.0D, 1.5D).shouldReject(10.0D, 10.0D));
   }

   @Test
   void hitModeDoesNotApplyPlaceDamageRatio() {
      assertFalse(inputs(true, false, false, false, false, false, false,
         20.0D, 20.0D, 10.0D, 20.0D, 100.0D).shouldReject(10.0D, 10.0D));
   }

   @Test
   void targetLethalityBypassesMinimumDamageAndMaximumSelfDamage() {
      assertFalse(inputs(false, false, false, false, false, false, false,
         20.0D, 10.0D, 100.0D, 0.0D, 0.0D).shouldReject(100.0D, 10.0D));
   }

   @Test
   void nonLethalDamageMustMeetBothThresholds() {
      assertTrue(inputs(false, false, false, false, false, false, false,
         20.0D, 10.0D, 8.0D, 5.0D, 0.0D).shouldReject(6.0D, 7.0D));
      assertTrue(inputs(false, false, false, false, false, false, false,
         20.0D, 10.0D, 8.0D, 5.0D, 0.0D).shouldReject(6.0D, 8.0D));
      assertFalse(inputs(false, false, false, false, false, false, false,
         20.0D, 10.0D, 8.0D, 6.0D, 0.0D).shouldReject(6.0D, 8.0D));
   }

   @Test
   void keepsNegatedComparisonsConservativeForNanDamage() {
      assertTrue(inputs(false, false, false, false, false, false, false,
         20.0D, 10.0D, 8.0D, 20.0D, 0.0D).shouldReject(1.0D, Double.NaN));
   }

   private static CrystalDamageSafetyInputs inputs(
      boolean hit,
      boolean selfInvulnerable,
      boolean forceSuicide,
      boolean antiSuicide,
      boolean selfHoldingTotem,
      boolean targetDamageIsKill,
      boolean targetDamageIsIntersection,
      double selfHealth,
      double targetHealth,
      double minimumTargetDamage,
      double maximumSelfDamage,
      double minimumRatio
   ) {
      return new CrystalDamageSafetyInputs(
         hit,
         selfInvulnerable,
         forceSuicide,
         antiSuicide,
         selfHoldingTotem,
         targetDamageIsKill,
         targetDamageIsIntersection,
         selfHealth,
         targetHealth,
         minimumTargetDamage,
         maximumSelfDamage,
         minimumRatio
      );
   }
}
