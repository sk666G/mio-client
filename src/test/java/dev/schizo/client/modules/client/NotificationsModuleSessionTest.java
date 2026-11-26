package dev.schizo.client.modules.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.schizo.client.events.MinecraftDisconnectEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class NotificationsModuleSessionTest {
   @Test
   void disconnectInvalidatesDelayedTotemWorkGeneration() throws Exception {
      NotificationsModule module = new NotificationsModule();

      Field generation = NotificationsModule.class.getDeclaredField("totemGeneration");
      generation.setAccessible(true);
      long before = generation.getLong(module);

      Method disconnect = NotificationsModule.class.getDeclaredMethod(
         "onMinecraftDisconnect",
         MinecraftDisconnectEvent.class
      );
      disconnect.setAccessible(true);
      disconnect.invoke(module, new MinecraftDisconnectEvent());

      assertEquals(before + 1L, generation.getLong(module));
   }
}
