package dev.schizo.client.modules.render;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.Map;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class TrajectoriesLifecycleTest {
   @Test
   void sessionResetClearsTrailAndPredictionState() throws Exception {
      TrajectoriesModule module = new TrajectoriesModule();

      @SuppressWarnings("unchecked")
      Map<Integer, ?> trails = (Map<Integer, ?>)getField(module, "trailsByEntityId");
      ((Map<Integer, Object>)trails).put(7, new java.util.ArrayList<Vec3>());
      setField(module, "previousPlayerVelocity", new Vec3(1.0D, 2.0D, 3.0D));
      setField(module, "previousBowPower", 0.75F);

      module.resetSessionState();

      assertTrue(trails.isEmpty());
      assertNull(getField(module, "trailLevel"));
      assertEquals(Vec3.ZERO, module.getPreviousPlayerVelocity());
      assertEquals(0.0F, module.getPreviousBowPower());
   }

   private static void setField(Object target, String name, Object value) throws Exception {
      Field field = target.getClass().getDeclaredField(name);
      field.setAccessible(true);
      field.set(target, value);
   }

   private static Object getField(Object target, String name) throws Exception {
      Field field = target.getClass().getDeclaredField(name);
      field.setAccessible(true);
      return field.get(target);
   }
}
