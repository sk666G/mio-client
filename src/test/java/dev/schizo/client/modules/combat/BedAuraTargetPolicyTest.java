package dev.schizo.client.modules.combat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BedAuraTargetPolicyTest {
   @Test
   void acceptsOnlyLiveNonCreativeNonSpectatorEnemies() {
      assertTrue(BedAuraModule.isEligibleTarget(false, true, false, false, false, false));
   }

   @Test
   void rejectsLocalPlayerFriendsAndServerSideDeadPlayers() {
      assertFalse(BedAuraModule.isEligibleTarget(true, true, false, false, false, false));
      assertFalse(BedAuraModule.isEligibleTarget(false, true, false, false, true, false));
      assertFalse(BedAuraModule.isEligibleTarget(false, true, false, false, false, true));
   }

   @Test
   void rejectsDeadCreativeAndSpectatorPlayers() {
      assertFalse(BedAuraModule.isEligibleTarget(false, false, false, false, false, false));
      assertFalse(BedAuraModule.isEligibleTarget(false, true, true, false, false, false));
      assertFalse(BedAuraModule.isEligibleTarget(false, true, false, true, false, false));
   }
}
