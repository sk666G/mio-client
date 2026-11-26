package dev.schizo.client.modules.combat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AutoLogDisconnectPolicyTest {
   @Test
   void keepsAutoLogSubscribedWhileDeferredDisconnectIsPending() {
      assertFalse(AutoLogModule.shouldDisableImmediately(true, true));
      assertTrue(AutoLogModule.shouldDisableAfterPendingDisconnect(true, true));
   }

   @Test
   void immediateDisconnectStillHonorsAutoDisable() {
      assertTrue(AutoLogModule.shouldDisableImmediately(true, false));
      assertFalse(AutoLogModule.shouldDisableAfterPendingDisconnect(true, false));
   }

   @Test
   void doesNotDisableWhenAutoDisableIsOff() {
      assertFalse(AutoLogModule.shouldDisableImmediately(false, false));
      assertFalse(AutoLogModule.shouldDisableImmediately(false, true));
      assertFalse(AutoLogModule.shouldDisableAfterPendingDisconnect(false, true));
   }
}
