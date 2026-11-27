package dev.schizo.client.utils.combat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TargetSelectorPolicyTest {
   @Test
   void acceptsAliveOrdinaryPlayers() {
      assertTrue(TargetSelector.isEligiblePlayerState(true, false, false, false, false));
   }

   @Test
   void rejectsDeadDyingSpectatorCreativeAndServerSideDeadPlayers() {
      assertFalse(TargetSelector.isEligiblePlayerState(false, false, false, false, false));
      assertFalse(TargetSelector.isEligiblePlayerState(true, true, false, false, false));
      assertFalse(TargetSelector.isEligiblePlayerState(true, false, true, false, false));
      assertFalse(TargetSelector.isEligiblePlayerState(true, false, false, true, false));
      assertFalse(TargetSelector.isEligiblePlayerState(true, false, false, false, true));
   }
}
