package dev.schizo.client.modules.combat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PusherPowerPolicyTest {
   @Test
   void acceptsEitherSupportedPowerItem() {
      assertTrue(PusherPowerPolicy.hasPowerSource(true, false));
      assertTrue(PusherPowerPolicy.hasPowerSource(false, true));
      assertTrue(PusherPowerPolicy.hasPowerSource(true, true));
   }

   @Test
   void rejectsAPlayerWithoutAnyPowerItem() {
      assertFalse(PusherPowerPolicy.hasPowerSource(false, false));
   }
}
