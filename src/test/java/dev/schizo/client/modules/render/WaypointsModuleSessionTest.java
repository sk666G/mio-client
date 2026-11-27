package dev.schizo.client.modules.render;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.schizo.client.events.MinecraftDisconnectEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class WaypointsModuleSessionTest {
   @Test
   void disableAndDisconnectClearVisibleWaypointSnapshot() throws Exception {
      WaypointsModule module = new WaypointsModule();
      Field visibleWaypoints = WaypointsModule.class.getDeclaredField("visibleWaypoints");
      visibleWaypoints.setAccessible(true);

      visibleWaypoints.set(module, Collections.singletonList(null));
      module.onDisable();
      assertTrue(((List<?>)visibleWaypoints.get(module)).isEmpty());

      visibleWaypoints.set(module, Collections.singletonList(null));
      Method disconnect = WaypointsModule.class.getDeclaredMethod(
         "onMinecraftDisconnectEvent",
         MinecraftDisconnectEvent.class
      );
      disconnect.setAccessible(true);
      disconnect.invoke(module, new MinecraftDisconnectEvent());
      assertTrue(((List<?>)visibleWaypoints.get(module)).isEmpty());
   }
}
