package dev.schizo.client.modules.player;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.render.GameOptionsOverrides;
import dev.schizo.client.settings.NamedEntry;
import java.lang.reflect.Field;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FreecamLifecycleTest {
   @Test
   void cleanupReleasesCameraOwnershipAfterPartialEnableOrWorldLoss() throws Exception {
      FreecamModule module = new FreecamModule();
      Field ownersField = GameOptionsOverrides.class.getDeclaredField("CAMERA_OVERRIDE_OWNERS");
      ownersField.setAccessible(true);

      @SuppressWarnings("unchecked")
      Set<NamedEntry> owners = (Set<NamedEntry>)ownersField.get(null);
      owners.add(module);
      try {
         assertDoesNotThrow(module::onDisable);
         assertFalse(owners.contains(module));
      } finally {
         owners.remove(module);
      }
   }

   @Test
   void rotationRestorationRequiresSameCapturedPlayerIdentity() {
      Object captured = new Object();
      Object replacement = new Object();

      assertTrue(FreecamModule.shouldRestorePlayerRotation(captured, captured));
      assertFalse(FreecamModule.shouldRestorePlayerRotation(captured, replacement));
      assertFalse(FreecamModule.shouldRestorePlayerRotation(null, replacement));
      assertFalse(FreecamModule.shouldRestorePlayerRotation(captured, null));
      assertTrue(FreecamModule.ownsCurrentPlayer(captured, captured));
      assertFalse(FreecamModule.ownsCurrentPlayer(captured, replacement));
   }

}
