package dev.schizo.client.modules;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DeferredModuleActionGuardTest {
   @Test
   void acceptsOnlyTheExactEnabledGenerationAndSession() {
      Object level = new Object();
      Object player = new Object();

      assertTrue(DeferredModuleActionGuard.isCurrent(
         true, 7L, 7L, level, level, player, player
      ));
      assertFalse(DeferredModuleActionGuard.isCurrent(
         false, 7L, 7L, level, level, player, player
      ));
      assertFalse(DeferredModuleActionGuard.isCurrent(
         true, 7L, 8L, level, level, player, player
      ));
      assertFalse(DeferredModuleActionGuard.isCurrent(
         true, 7L, 7L, level, new Object(), player, player
      ));
      assertFalse(DeferredModuleActionGuard.isCurrent(
         true, 7L, 7L, level, level, player, new Object()
      ));
      assertFalse(DeferredModuleActionGuard.isCurrent(
         true, 7L, 7L, null, null, player, player
      ));
      assertFalse(DeferredModuleActionGuard.isCurrent(
         true, 7L, 7L, level, level, null, null
      ));
   }
}
