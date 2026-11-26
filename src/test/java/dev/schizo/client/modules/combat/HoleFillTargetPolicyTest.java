package dev.schizo.client.modules.combat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HoleFillTargetPolicyTest {
   @Test
   void considersOnlyLiveNonCreativeNonSpectatorEnemies() {
      assertTrue(HoleFillTargetPolicy.isRelevant(true, false, false, false, false, false, false));
   }

   @Test
   void rejectsDeadOrDyingLocalSpectatorCreativeFriendAndServerSideDeadPlayers() {
      assertFalse(HoleFillTargetPolicy.isRelevant(false, false, false, false, false, false, false));
      assertFalse(HoleFillTargetPolicy.isRelevant(true, true, false, false, false, false, false));
      assertFalse(HoleFillTargetPolicy.isRelevant(true, false, true, false, false, false, false));
      assertFalse(HoleFillTargetPolicy.isRelevant(true, false, false, true, false, false, false));
      assertFalse(HoleFillTargetPolicy.isRelevant(true, false, false, false, true, false, false));
      assertFalse(HoleFillTargetPolicy.isRelevant(true, false, false, false, false, true, false));
      assertFalse(HoleFillTargetPolicy.isRelevant(true, false, false, false, false, false, true));
   }
}
