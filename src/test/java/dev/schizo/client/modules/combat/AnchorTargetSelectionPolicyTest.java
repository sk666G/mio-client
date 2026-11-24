package dev.schizo.client.modules.combat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AnchorTargetSelectionPolicyTest {
   @Test
   void acceptsLiveEligibleEnemiesAtTheExactRangeBoundary() {
      assertFalse(AnchorTargetSelectionPolicy.isInvalidTarget(
         target(false, true, false, false, false, false, 6.0D),
         6.0D
      ));
   }

   @Test
   void rejectsLocalDeadFriendServerDeadSpectatorAndCreativeTargets() {
      assertTrue(invalid(target(true, true, false, false, false, false, 1.0D)));
      assertTrue(invalid(target(false, false, false, false, false, false, 1.0D)));
      assertTrue(invalid(target(false, true, true, false, false, false, 1.0D)));
      assertTrue(invalid(target(false, true, false, true, false, false, 1.0D)));
      assertTrue(invalid(target(false, true, false, false, true, false, 1.0D)));
      assertTrue(invalid(target(false, true, false, false, false, true, 1.0D)));
   }

   @Test
   void rejectsNonFiniteAndNegativeRangesOrDistances() {
      assertTrue(AnchorTargetSelectionPolicy.isInvalidTarget(
         target(false, true, false, false, false, false, 1.0D),
         Double.NaN
      ));
      assertTrue(AnchorTargetSelectionPolicy.isInvalidTarget(
         target(false, true, false, false, false, false, 1.0D),
         Double.POSITIVE_INFINITY
      ));
      assertTrue(invalid(target(false, true, false, false, false, false, Double.NaN)));
      assertTrue(invalid(target(false, true, false, false, false, false, Double.POSITIVE_INFINITY)));
      assertTrue(invalid(target(false, true, false, false, false, false, -1.0D)));
   }

   @Test
   void rejectsNullTargetState() {
      assertTrue(AnchorTargetSelectionPolicy.isInvalidTarget(null, 6.0D));
   }

   @Test
   void acceptsDamageAtBothExactThresholds() {
      assertTrue(AnchorTargetSelectionPolicy.acceptsDamage(0.5D, 0.1D));
      assertTrue(AnchorTargetSelectionPolicy.acceptsDamage(6.0D, 6.0D));
   }

   @Test
   void rejectsDamageBelowEitherThreshold() {
      assertFalse(AnchorTargetSelectionPolicy.acceptsDamage(0.49D, 0.1D));
      assertFalse(AnchorTargetSelectionPolicy.acceptsDamage(5.99D, 6.0D));
   }

   @Test
   void rejectsNonFiniteOrNegativeDamageAndMinimumValues() {
      assertFalse(AnchorTargetSelectionPolicy.acceptsDamage(Double.NaN, 1.0D));
      assertFalse(AnchorTargetSelectionPolicy.acceptsDamage(Double.POSITIVE_INFINITY, 1.0D));
      assertFalse(AnchorTargetSelectionPolicy.acceptsDamage(-1.0D, 0.0D));
      assertFalse(AnchorTargetSelectionPolicy.acceptsDamage(5.0D, Double.NaN));
      assertFalse(AnchorTargetSelectionPolicy.acceptsDamage(5.0D, Double.POSITIVE_INFINITY));
      assertFalse(AnchorTargetSelectionPolicy.acceptsDamage(5.0D, -1.0D));
   }

   private static boolean invalid(AnchorTargetSelectionPolicy.TargetState target) {
      return AnchorTargetSelectionPolicy.isInvalidTarget(target, 6.0D);
   }

   private static AnchorTargetSelectionPolicy.TargetState target(
      boolean localPlayer,
      boolean alive,
      boolean friend,
      boolean serverSideDead,
      boolean spectator,
      boolean creative,
      double distance
   ) {
      return new AnchorTargetSelectionPolicy.TargetState(
         localPlayer, alive, friend, serverSideDead, spectator, creative, distance
      );
   }
}
