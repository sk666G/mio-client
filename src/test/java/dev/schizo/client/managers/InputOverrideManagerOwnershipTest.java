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
         assertTrue(isPending(manager, "pendingSneak"));

         manager.clearSneakRequest(otherOwner);
         assertTrue(isPending(manager, "pendingSneak"));

         manager.clearSneakRequest(owner);
         assertFalse(isPending(manager, "pendingSneak"));
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

         assertTrue(isPending(manager, "pendingSneak"));
      } finally {
         ClientAccess.eventBus.unsubscribe(manager);
      }
   }

   @Test
   void clearRequestsClearsAllPendingValuesOwnedByRequester() throws Exception {
      InputOverrideManager manager = new InputOverrideManager();
      Object owner = new Object();
      try {
         manager.requestSneak(owner, true);
         manager.requestJump(owner, true);
         manager.requestForward(owner, true);
         manager.requestBack(owner, true);
         manager.requestLeft(owner, true);
         manager.requestRight(owner, true);
         manager.requestSprint(owner, true);
         manager.requestForwardMovement(owner, 1.0F);
         manager.requestSidewaysMovement(owner, -1.0F);

         manager.clearRequests(owner);

         for (String field : new String[]{
            "pendingSneak", "pendingJump", "pendingForward", "pendingBack",
            "pendingLeft", "pendingRight", "pendingSprint",
            "pendingForwardMovement", "pendingSidewaysMovement"
         }) {
            assertFalse(isPending(manager, field), field);
         }
      } finally {
         ClientAccess.eventBus.unsubscribe(manager);
      }
   }

   @Test
   void clearRequestsCannotClearLaterAnonymousOrOtherOwnedValues() throws Exception {
      InputOverrideManager manager = new InputOverrideManager();
      Object owner = new Object();
      Object otherOwner = new Object();
      try {
         manager.requestForward(owner, true);
         manager.requestForward(otherOwner, false);
         manager.requestJump(owner, true);
         manager.requestJump(false);

         manager.clearRequests(owner);

         assertTrue(isPending(manager, "pendingForward"));
         assertTrue(isPending(manager, "pendingJump"));
      } finally {
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
