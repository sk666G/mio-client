package dev.schizo.client.modules.player;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.render.GameOptionsOverrides;
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
      Set<String> owners = (Set<String>)ownersField.get(null);
      String ownerName = module.getName();
      owners.add(ownerName);
      try {
         assertDoesNotThrow(module::onDisable);
         assertFalse(owners.contains(ownerName));
      } finally {
         owners.remove(ownerName);
      }
   }
}
