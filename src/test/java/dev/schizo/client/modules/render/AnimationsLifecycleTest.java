package dev.schizo.client.modules.render;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class AnimationsLifecycleTest {
   @Test
   void smoothCameraStateDoesNotSurviveLifecycleReset() throws Exception {
      AnimationsModule module = new AnimationsModule();

      setField(module, "verticalCameraCatchUp", true);
      setField(module, "horizontalCameraCatchUp", true);
      setField(module, "cameraPosition", new Vec3(10.0D, 20.0D, 30.0D));
      setField(module, "previousCameraPosition", new Vec3(9.0D, 19.0D, 29.0D));

      module.resetSmoothCameraState();

      assertFalse((Boolean)getField(module, "verticalCameraCatchUp"));
      assertFalse((Boolean)getField(module, "horizontalCameraCatchUp"));
      assertNull(getField(module, "cameraPosition"));
      assertNull(getField(module, "previousCameraPosition"));
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
