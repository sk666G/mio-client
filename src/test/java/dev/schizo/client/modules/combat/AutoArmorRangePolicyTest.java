package dev.schizo.client.modules.combat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AutoArmorRangePolicyTest {
   @Test
   void enemiesAlwaysCountTowardTheRangeTrigger() {
      assertTrue(AutoArmorModule.shouldCountPlayerForRange(false, false));
      assertTrue(AutoArmorModule.shouldCountPlayerForRange(false, true));
   }

   @Test
   void friendsCountOnlyWhenAllowFriendsIsEnabled() {
      assertFalse(AutoArmorModule.shouldCountPlayerForRange(true, false));
      assertTrue(AutoArmorModule.shouldCountPlayerForRange(true, true));
   }
}
