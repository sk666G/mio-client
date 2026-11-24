package dev.schizo.client.modules.combat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PusherTargetPolicyTest {
   @Test
   void acceptsLiveEnemyPlayers() {
      assertTrue(PusherTargetPolicy.isEligible(true, false, false, false, false, false));
   }

   @Test
   void rejectsDeadLocalSpectatorCreativeFriendAndServerSideDeadPlayers() {
      assertFalse(PusherTargetPolicy.isEligible(false, false, false, false, false, false));
      assertFalse(PusherTargetPolicy.isEligible(true, true, false, false, false, false));
      assertFalse(PusherTargetPolicy.isEligible(true, false, true, false, false, false));
      assertFalse(PusherTargetPolicy.isEligible(true, false, false, true, false, false));
      assertFalse(PusherTargetPolicy.isEligible(true, false, false, false, true, false));
      assertFalse(PusherTargetPolicy.isEligible(true, false, false, false, false, true));
   }
}
