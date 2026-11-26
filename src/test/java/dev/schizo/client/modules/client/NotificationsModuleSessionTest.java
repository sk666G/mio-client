package dev.schizo.client.modules.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import dev.schizo.client.events.MinecraftDisconnectEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class NotificationsModuleSessionTest {
   @Test
   void disconnectInvalidatesDelayedTotemWorkGeneration() throws Exception {
      NotificationsModule module = new NotificationsModule();

      Field generation = NotificationsModule.class.getDeclaredField("totemGeneration");
      generation.setAccessible(true);
      long before = generation.getLong(module);

      Field pendingChats = NotificationsModule.class.getDeclaredField("pendingTotemChats");
      pendingChats.setAccessible(true);
      AtomicInteger oldCounter = (AtomicInteger)pendingChats.get(module);
      oldCounter.set(64);

      Method disconnect = NotificationsModule.class.getDeclaredMethod(
         "onMinecraftDisconnect",
         MinecraftDisconnectEvent.class
      );
      disconnect.setAccessible(true);
      disconnect.invoke(module, new MinecraftDisconnectEvent());

      assertEquals(before + 1L, generation.getLong(module));
      AtomicInteger newCounter = (AtomicInteger)pendingChats.get(module);
      assertNotSame(oldCounter, newCounter);
      assertEquals(0, newCounter.get());
      assertEquals(64, oldCounter.get());
   }
}
