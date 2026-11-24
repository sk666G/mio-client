package dev.schizo.client.modules.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CrystalTargetSelectionPolicyTest {
   @Test
   void forceSuicideOnlyAcceptsTheLocalPlayerAndBypassesOtherTargetChecks() {
      CrystalTargetSelectionPolicy.TargetState selfButDead = target(
         true, false, true, true, false, true, true, true, 999.0D
      );
      CrystalTargetSelectionPolicy.TargetState enemy = target(
         false, true, false, false, true, false, false, false, 1.0D
      );

      assertFalse(CrystalTargetSelectionPolicy.isInvalidTarget(selfButDead, true, 4.0D));
      assertTrue(CrystalTargetSelectionPolicy.isInvalidTarget(enemy, true, 4.0D));
   }

   @Test
   void acceptsOnlyLiveNonFriendNonCreativeInRangeEnemies() {
      assertFalse(CrystalTargetSelectionPolicy.isInvalidTarget(
         target(false, true, false, false, true, false, false, false, 3.0D),
         false,
         4.0D
      ));
   }

   @Test
   void rejectsEachOrdinaryInvalidTargetState() {
      assertTrue(invalid(target(false, true, true, false, true, false, false, false, 1.0D)));
      assertTrue(invalid(target(false, true, false, true, true, false, false, false, 1.0D)));
      assertTrue(invalid(target(false, false, false, false, false, false, false, false, 1.0D)));
      assertTrue(invalid(target(false, false, false, false, true, true, false, false, 1.0D)));
      assertTrue(invalid(target(false, false, false, false, true, false, true, false, 1.0D)));
      assertTrue(invalid(target(false, false, false, false, true, false, false, true, 1.0D)));
      assertTrue(invalid(target(true, false, false, false, true, false, false, false, 1.0D)));
      assertTrue(invalid(target(false, false, false, false, true, false, false, false, 5.0D)));
   }

   @Test
   void rejectsInsufficientVerticalClearanceWithoutAnExistingCrystalBase() {
      assertEquals(
         CrystalTargetSelectionPolicy.Decision.REJECT,
         candidate(6.6D, 5.0D, false, false, true, 1.0D, 4.0D,
            false, false, false, false, false, 10.0D, 20.0D, 1.0D)
      );
   }

   @Test
   void allowsInsufficientVerticalClearanceWhenBaseAlreadyContainsCrystal() {
      assertEquals(
         CrystalTargetSelectionPolicy.Decision.ACCEPT,
         candidate(6.6D, 5.0D, true, false, true, 1.0D, 4.0D,
            false, false, false, false, false, 10.0D, 20.0D, 1.0D)
      );
   }

   @Test
   void ignoreNakedsRequiresArmorOrAnEndCrystalInHand() {
      assertEquals(
         CrystalTargetSelectionPolicy.Decision.REJECT,
         candidate(5.0D, 5.0D, false, true, false, false, 1.0D, 4.0D,
            false, false, false, false, false, 10.0D, 20.0D, 1.0D)
      );
      assertEquals(
         CrystalTargetSelectionPolicy.Decision.ACCEPT,
         candidate(5.0D, 5.0D, false, true, false, true, 1.0D, 4.0D,
            false, false, false, false, false, 10.0D, 20.0D, 1.0D)
      );
      assertEquals(
         CrystalTargetSelectionPolicy.Decision.ACCEPT,
         candidate(5.0D, 5.0D, false, true, true, false, 1.0D, 4.0D,
            false, false, false, false, false, 10.0D, 20.0D, 1.0D)
      );
   }

   @Test
   void lethalAndThreatOverridesBypassOnlyCrystalRangeFilter() {
      assertEquals(
         CrystalTargetSelectionPolicy.Decision.REJECT,
         candidate(5.0D, 5.0D, false, false, true, false, 5.0D, 4.0D,
            false, false, false, false, false, 10.0D, 20.0D, 1.0D)
      );
      assertEquals(
         CrystalTargetSelectionPolicy.Decision.ACCEPT,
         candidate(5.0D, 5.0D, false, false, true, false, 5.0D, 4.0D,
            true, false, false, false, false, 10.0D, 20.0D, 1.0D)
      );
      assertEquals(
         CrystalTargetSelectionPolicy.Decision.ACCEPT,
         candidate(5.0D, 5.0D, false, false, true, false, 5.0D, 4.0D,
            false, true, false, false, false, 10.0D, 20.0D, 1.0D)
      );
      assertEquals(
         CrystalTargetSelectionPolicy.Decision.ACCEPT,
         candidate(5.0D, 5.0D, false, false, true, false, 5.0D, 4.0D,
            false, false, true, false, false, 10.0D, 20.0D, 1.0D)
      );
   }

   @Test
   void friendEntitiesAreSkippedAndFriendKillCanAbortWholePass() {
      assertEquals(
         CrystalTargetSelectionPolicy.Decision.REJECT,
         candidate(5.0D, 5.0D, false, false, true, false, 1.0D, 4.0D,
            false, false, false, true, false, 10.0D, 20.0D, 1.0D)
      );
      assertEquals(
         CrystalTargetSelectionPolicy.Decision.ABORT_SCAN,
         candidate(5.0D, 5.0D, false, false, true, false, 1.0D, 4.0D,
            true, false, false, true, true, 21.0D, 20.0D, 1.0D)
      );
      assertEquals(
         CrystalTargetSelectionPolicy.Decision.REJECT,
         candidate(5.0D, 5.0D, false, false, true, false, 1.0D, 4.0D,
            true, false, false, true, true, 20.0D, 20.0D, 1.0D)
      );
   }

   @Test
   void nonLethalDamageMustMeetMinimumAndHalfPointFloor() {
      assertEquals(
         CrystalTargetSelectionPolicy.Decision.REJECT,
         candidate(5.0D, 5.0D, false, false, true, false, 1.0D, 4.0D,
            false, false, false, false, false, 4.0D, 20.0D, 5.0D)
      );
      assertEquals(
         CrystalTargetSelectionPolicy.Decision.REJECT,
         candidate(5.0D, 5.0D, false, false, true, false, 1.0D, 4.0D,
            false, false, false, false, false, 0.4D, 20.0D, 0.1D)
      );
      assertEquals(
         CrystalTargetSelectionPolicy.Decision.ACCEPT,
         candidate(5.0D, 5.0D, false, false, true, false, 1.0D, 4.0D,
            true, false, false, false, false, 0.4D, 20.0D, 5.0D)
      );
   }

   @Test
   void tiesDoNotReplaceEarlierBestTarget() {
      assertFalse(CrystalTargetSelectionPolicy.shouldReplaceBest(5.0D, 5.0D));
      assertFalse(CrystalTargetSelectionPolicy.shouldReplaceBest(4.0D, 5.0D));
      assertTrue(CrystalTargetSelectionPolicy.shouldReplaceBest(5.1D, 5.0D));
   }

   private static boolean invalid(CrystalTargetSelectionPolicy.TargetState state) {
      return CrystalTargetSelectionPolicy.isInvalidTarget(state, false, 4.0D);
   }

   private static CrystalTargetSelectionPolicy.TargetState target(
      boolean self,
      boolean player,
      boolean friend,
      boolean creative,
      boolean alive,
      boolean dying,
      boolean serverDead,
      boolean spectator,
      double distance
   ) {
      return new CrystalTargetSelectionPolicy.TargetState(
         self, player, friend, creative, alive, dying, serverDead, spectator, distance
      );
   }

   private static CrystalTargetSelectionPolicy.Decision candidate(
      double baseY,
      double targetY,
      boolean hasCrystalBase,
      boolean ignoreNakeds,
      boolean targetHasArmor,
      boolean targetHoldingCrystal,
      double distanceToBase,
      double crystalRange,
      boolean lethal,
      boolean obsidianDrop,
      boolean quiver,
      boolean friendEntity,
      boolean antiFriendKill,
      double damage,
      double targetHealth,
      double minimumDamage
   ) {
      return CrystalTargetSelectionPolicy.evaluateCandidate(
         new CrystalTargetSelectionPolicy.CandidateState(
            baseY, targetY, hasCrystalBase, ignoreNakeds, targetHasArmor, targetHoldingCrystal,
            distanceToBase, crystalRange, lethal, obsidianDrop, quiver, friendEntity,
            antiFriendKill, damage, targetHealth, minimumDamage
         )
      );
   }
}
