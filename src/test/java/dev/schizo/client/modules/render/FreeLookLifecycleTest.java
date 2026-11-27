package dev.schizo.client.modules.render;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.ClientAccess;
import org.junit.jupiter.api.Test;

class FreeLookLifecycleTest {
   @Test
   void cleanupIsSafeWithoutAWorldOrPlayer() {
      FreeLookModule module = new FreeLookModule();
      assertDoesNotThrow(module::onDisable);
   }

   @Test
   void rotationRestorationRequiresSameCapturedPlayerIdentity() {
      Object captured = new Object();
      Object replacement = new Object();

      assertTrue(FreeLookModule.shouldRestorePlayerRotation(captured, captured));
      assertFalse(FreeLookModule.shouldRestorePlayerRotation(captured, replacement));
      assertFalse(FreeLookModule.shouldRestorePlayerRotation(null, replacement));
      assertFalse(FreeLookModule.shouldRestorePlayerRotation(captured, null));
      assertTrue(FreeLookModule.ownsCurrentPlayer(captured, captured));
      assertFalse(FreeLookModule.ownsCurrentPlayer(captured, replacement));
   }

}
