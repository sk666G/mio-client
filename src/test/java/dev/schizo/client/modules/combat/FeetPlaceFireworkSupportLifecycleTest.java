package dev.schizo.client.modules.combat;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FeetPlaceFireworkSupportLifecycleTest {
   @Test
   void enableClearsAllTrackingState() throws Exception {
      FeetPlaceFireworkSupport support = new FeetPlaceFireworkSupport();
      Map<Object, Object> pending = mapField(support, "pendingFireworks");
      Map<Object, Object> spawnTimes = mapField(support, "itemSpawnTimes");
      pending.put("pending", "value");
      spawnTimes.put("spawn", new ArrayDeque<>());

      support.onEnable();

      assertTrue(pending.isEmpty());
      assertTrue(spawnTimes.isEmpty());
   }

   @Test
   void disableClearsAllTrackingState() throws Exception {
      FeetPlaceFireworkSupport support = new FeetPlaceFireworkSupport();
      Map<Object, Object> pending = mapField(support, "pendingFireworks");
      Map<Object, Object> spawnTimes = mapField(support, "itemSpawnTimes");
      pending.put("pending", "value");
      spawnTimes.put("spawn", new ArrayDeque<>());

      support.onDisable();

      assertTrue(pending.isEmpty());
      assertTrue(spawnTimes.isEmpty());
   }

   @SuppressWarnings("unchecked")
   private static Map<Object, Object> mapField(FeetPlaceFireworkSupport support, String name)
      throws ReflectiveOperationException {
      Field field = FeetPlaceFireworkSupport.class.getDeclaredField(name);
      field.setAccessible(true);
      return (Map<Object, Object>)field.get(support);
   }
}
