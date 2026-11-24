package dev.schizo.client.modules.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CrystalDamageThresholdsTest {
   @Test
   void obsidianDropAndQuiverThreatOverridesAllOtherThresholds() {
      CrystalDamageThresholds thresholds = thresholds(
         true, true, true, true, 20.0D, 10.0D, 12.0D, false, true, true
      );

      assertEquals(0.1D, thresholds.minimumDamage(false));
      assertEquals(0.1D, thresholds.minimumDamage(true));
   }

   @Test
   void facePlaceAndNetherPortalUseTheLowThreshold() {
      assertEquals(0.6D, thresholds(
         false, false, true, false, 20.0D, 10.0D, 12.0D, false, false, false
      ).minimumDamage(false));
      assertEquals(0.6D, thresholds(
         false, false, false, true, 20.0D, 10.0D, 12.0D, false, false, false
      ).minimumDamage(true));
   }

   @Test
   void selectsPlaceAndHitThresholdsIndependently() {
      CrystalDamageThresholds thresholds = thresholds(
         false, false, false, false, 20.0D, 4.0D, 7.0D, false, false, false
      );

      assertEquals(4.0D, thresholds.minimumDamage(false));
      assertEquals(7.0D, thresholds.minimumDamage(true));
   }

   @Test
   void boundaryLabelUsesTargetHealthOnlyForPlaceThreshold() {
      CrystalDamageThresholds thresholds = thresholds(
         false, false, false, false, 6.0D, 4.0D, 8.0D, true, false, false
      );

      assertEquals(6.0D, thresholds.minimumDamage(false));
      assertEquals(6.0D, thresholds.minimumDamage(true));
   }

   @Test
   void miningPolicyLowersPlaceThresholdAndSetsHitThresholdToThree() {
      CrystalDamageThresholds thresholds = thresholds(
         false, false, false, false, 20.0D, 8.0D, 12.0D, false, true, false
      );

      assertEquals(5.0D, thresholds.minimumDamage(false));
      assertEquals(3.0D, thresholds.minimumDamage(true));
   }

   @Test
   void alwaysIgnoreMiningUsesPointSixAsPlaceFloor() {
      CrystalDamageThresholds thresholds = thresholds(
         false, false, false, false, 20.0D, 8.0D, 12.0D, false, true, true
      );

      assertEquals(0.6D, thresholds.minimumDamage(false));
      assertEquals(3.0D, thresholds.minimumDamage(true));
   }

   @Test
   void thresholdNeverExceedsTargetHealth() {
      CrystalDamageThresholds thresholds = thresholds(
         false, false, false, false, 2.0D, 8.0D, 12.0D, false, false, false
      );

      assertEquals(2.0D, thresholds.minimumDamage(false));
      assertEquals(2.0D, thresholds.minimumDamage(true));
   }

   private static CrystalDamageThresholds thresholds(
      boolean obsidianDrop,
      boolean quiver,
      boolean facePlace,
      boolean portal,
      double health,
      double placeMinimum,
      double hitMinimum,
      boolean placeUsesHealth,
      boolean ignoreMining,
      boolean alwaysIgnoreMining
   ) {
      return new CrystalDamageThresholds(
         obsidianDrop,
         quiver,
         facePlace,
         portal,
         health,
         placeMinimum,
         hitMinimum,
         placeUsesHealth,
         ignoreMining,
         alwaysIgnoreMining
      );
   }
}
