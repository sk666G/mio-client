package dev.schizo.client.modules.combat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CombatTargetPolicyTest {
   @Test
   void acceptsLiveNonCreativeNonSpectatorEnemies() {
      assertTrue(CombatTargetPolicy.isEligible(true, false, false, false, false, false, false));
   }

   @Test
   void rejectsInvalidCombatTargets() {
      assertFalse(CombatTargetPolicy.isEligible(false, false, false, false, false, false, false));
      assertFalse(CombatTargetPolicy.isEligible(true, true, false, false, false, false, false));
      assertFalse(CombatTargetPolicy.isEligible(true, false, true, false, false, false, false));
      assertFalse(CombatTargetPolicy.isEligible(true, false, false, true, false, false, false));
      assertFalse(CombatTargetPolicy.isEligible(true, false, false, false, true, false, false));
      assertFalse(CombatTargetPolicy.isEligible(true, false, false, false, false, true, false));
      assertFalse(CombatTargetPolicy.isEligible(true, false, false, false, false, false, true));
   }
}
