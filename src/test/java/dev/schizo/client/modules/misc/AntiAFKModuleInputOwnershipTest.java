package dev.schizo.client.modules.misc;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.ClientContext;
import dev.schizo.client.managers.InputOverrideManager;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class AntiAFKModuleInputOwnershipTest {
   @Test
   void disableClearsOnlyAntiAfkOwnedPendingInput() throws Exception {
      InputOverrideManager previous = ClientContext.inputOverrides;
      InputOverrideManager manager = new InputOverrideManager();
      AntiAFKModule module = new AntiAFKModule();
      Object otherOwner = new Object();
      ClientContext.inputOverrides = manager;
      try {
         manager.requestJump(module, true);
         manager.requestSneak(module, true);

         module.onDisable();

         assertFalse(isPending(manager, "pendingJump"));
         assertFalse(isPending(manager, "pendingSneak"));

         manager.requestJump(otherOwner, true);
         manager.requestSneak(otherOwner, true);

         module.onDisable();

         assertTrue(isPending(manager, "pendingJump"));
         assertTrue(isPending(manager, "pendingSneak"));
      } finally {
         ClientContext.inputOverrides = previous;
         ClientAccess.eventBus.unsubscribe(manager);
      }
   }

   private static boolean isPending(InputOverrideManager manager, String fieldName) throws Exception {
      Field pendingField = InputOverrideManager.class.getDeclaredField(fieldName);
      pendingField.setAccessible(true);
      Object pendingValue = pendingField.get(manager);

      Field pending = pendingValue.getClass().getSuperclass().getDeclaredField("pending");
      pending.setAccessible(true);
      return pending.getBoolean(pendingValue);
   }
}
