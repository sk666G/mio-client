package dev.schizo.client.modules.misc;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.events.MinecraftDisconnectEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class WebhookNotifsModuleLifecycleTest {
   @Test
   void disableClearsQueueSessionDedupeState() throws Exception {
      WebhookNotifsModule module = new WebhookNotifsModule();
      Field previousQueueMessage = previousQueueMessageField();
      previousQueueMessage.set(module, "Position in queue: 42");

      module.onDisable();

      assertNull(previousQueueMessage.get(module));
   }

   @Test
   void disconnectClearsQueueSessionDedupeState() throws Exception {
      WebhookNotifsModule module = new WebhookNotifsModule();
      Field previousQueueMessage = previousQueueMessageField();
      previousQueueMessage.set(module, "Position in queue: 42");

      Method disconnect = WebhookNotifsModule.class.getDeclaredMethod(
         "onMinecraftDisconnectEvent",
         MinecraftDisconnectEvent.class
      );
      disconnect.setAccessible(true);
      disconnect.invoke(module, new MinecraftDisconnectEvent());

      assertNull(previousQueueMessage.get(module));
   }

   private static Field previousQueueMessageField() throws Exception {
      Field field = WebhookNotifsModule.class.getDeclaredField("previousQueueMessage");
      field.setAccessible(true);
      return field;
   }
}
