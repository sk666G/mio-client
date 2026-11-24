package dev.schizo.client.modules.combat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AntiPhaseTargetPolicyTest {
   @Test
   void acceptsLiveNonSwimmingEnemyPlayers() {
      assertTrue(AntiPhaseTargetPolicy.isEligible(true, false, false, false, false, false, false));
   }

   @Test
   void rejectsInvalidTargets() {
      assertFalse(AntiPhaseTargetPolicy.isEligible(false, false, false, false, false, false, false));
      assertFalse(AntiPhaseTargetPolicy.isEligible(true, true, false, false, false, false, false));
      assertFalse(AntiPhaseTargetPolicy.isEligible(true, false, true, false, false, false, false));
      assertFalse(AntiPhaseTargetPolicy.isEligible(true, false, false, true, false, false, false));
      assertFalse(AntiPhaseTargetPolicy.isEligible(true, false, false, false, true, false, false));
      assertFalse(AntiPhaseTargetPolicy.isEligible(true, false, false, false, false, true, false));
      assertFalse(AntiPhaseTargetPolicy.isEligible(true, false, false, false, false, false, true));
   }
}
