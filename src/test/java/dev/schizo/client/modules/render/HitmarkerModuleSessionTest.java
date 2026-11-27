package dev.schizo.client.modules.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import dev.schizo.client.events.MinecraftDisconnectEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class HitmarkerModuleSessionTest {
   @Test
   void disconnectClearsTransientHitState() throws Exception {
      HitmarkerModule module = new HitmarkerModule();
      module.lastHitTime = 1234L;
      module.soundPending = true;

      Field lastHitNanos = HitmarkerModule.class.getDeclaredField("lastHitNanos");
      lastHitNanos.setAccessible(true);
      lastHitNanos.setLong(module, 5678L);

      Method disconnect = HitmarkerModule.class.getDeclaredMethod(
         "onMinecraftDisconnectEvent",
         MinecraftDisconnectEvent.class
      );
      disconnect.setAccessible(true);
      disconnect.invoke(module, new MinecraftDisconnectEvent());

      assertEquals(-1L, module.lastHitTime);
      assertEquals(-1L, lastHitNanos.getLong(module));
      assertFalse(module.soundPending);
   }
}
