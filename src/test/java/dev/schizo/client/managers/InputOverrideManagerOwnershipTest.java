package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.ClientAccess;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class InputOverrideManagerOwnershipTest {
   @Test
   void sneakCancellationOnlyClearsMatchingOwner() throws Exception {
      InputOverrideManager manager = new InputOverrideManager();
      Object owner = new Object();
      Object otherOwner = new Object();
      try {
         manager.requestSneak(owner, true);
         assertTrue(isSneakPending(manager));

         manager.clearSneakRequest(otherOwner);
         assertTrue(isSneakPending(manager));

         manager.clearSneakRequest(owner);
         assertFalse(isSneakPending(manager));
      } finally {
         ClientAccess.eventBus.unsubscribe(manager);
      }
   }

   @Test
   void ownerCannotClearLaterAnonymousSneakRequest() throws Exception {
      InputOverrideManager manager = new InputOverrideManager();
      Object owner = new Object();
      try {
         manager.requestSneak(owner, true);
         manager.requestSneak(false);

         manager.clearSneakRequest(owner);

         assertTrue(isSneakPending(manager));
      } finally {
         ClientAccess.eventBus.unsubscribe(manager);
      }
   }

   private static boolean isSneakPending(InputOverrideManager manager) throws Exception {
      Field pendingSneak = InputOverrideManager.class.getDeclaredField("pendingSneak");
      pendingSneak.setAccessible(true);
      Object pendingValue = pendingSneak.get(manager);

      Field pending = pendingValue.getClass().getSuperclass().getDeclaredField("pending");
      pending.setAccessible(true);
      return pending.getBoolean(pendingValue);
   }
}
