package dev.schizo.client.modules.misc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class VisualRangeSessionGuardTest {
   @Test
   void deferredNotificationRequiresSameEnabledLifecycleAndSession() {
      Object level = new Object();
      Object player = new Object();

      assertTrue(VisualRangeModule.ownsNotificationContext(
         true, 2L, 2L, level, level, player, player
      ));
      assertFalse(VisualRangeModule.ownsNotificationContext(
         false, 2L, 2L, level, level, player, player
      ));
      assertFalse(VisualRangeModule.ownsNotificationContext(
         true, 2L, 3L, level, level, player, player
      ));
      assertFalse(VisualRangeModule.ownsNotificationContext(
         true, 2L, 2L, level, new Object(), player, player
      ));
      assertFalse(VisualRangeModule.ownsNotificationContext(
         true, 2L, 2L, level, level, player, new Object()
      ));
   }
}
