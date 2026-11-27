package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.assertNull;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class MovementPredictionManagerSessionTest {
   @Test
   void disconnectClearsPreviousLocalPosition() throws Exception {
      MovementPredictionManager manager = new MovementPredictionManager();
      try {
         Field previousPosition = MovementPredictionManager.class.getDeclaredField("previousPlayerPosition");
         previousPosition.setAccessible(true);
         previousPosition.set(manager, new Vec3(128.0D, 64.0D, -256.0D));

         Method disconnect = MovementPredictionManager.class.getDeclaredMethod(
            "onMinecraftDisconnectEvent",
            MinecraftDisconnectEvent.class
         );
         disconnect.setAccessible(true);
         disconnect.invoke(manager, new MinecraftDisconnectEvent());

         assertNull(previousPosition.get(manager));
      } finally {
         ClientAccess.eventBus.unsubscribe(manager);
      }
   }
}
