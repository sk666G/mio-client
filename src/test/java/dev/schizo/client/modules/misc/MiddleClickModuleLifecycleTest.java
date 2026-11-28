package dev.schizo.client.modules.misc;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class MiddleClickModuleLifecycleTest {
   @Test
   void enableStartsWithFreshTransientInputState() throws Exception {
      MiddleClickModule module = new MiddleClickModule();
      setFlag(module, "middleButtonHeld", true);
      setFlag(module, "launchAgainNextTick", true);

      module.onEnable();

      assertFalse(flag(module, "middleButtonHeld"));
      assertFalse(flag(module, "launchAgainNextTick"));
   }

   @Test
   void disableClearsQueuedSecondLaunchAndHeldState() throws Exception {
      MiddleClickModule module = new MiddleClickModule();
      setFlag(module, "middleButtonHeld", true);
      setFlag(module, "launchAgainNextTick", true);

      module.onDisable();

      assertFalse(flag(module, "middleButtonHeld"));
      assertFalse(flag(module, "launchAgainNextTick"));
   }

   private static void setFlag(MiddleClickModule module, String name, boolean value)
      throws ReflectiveOperationException {
      Field field = MiddleClickModule.class.getDeclaredField(name);
      field.setAccessible(true);
      field.setBoolean(module, value);
   }

   private static boolean flag(MiddleClickModule module, String name)
      throws ReflectiveOperationException {
      Field field = MiddleClickModule.class.getDeclaredField(name);
      field.setAccessible(true);
      return field.getBoolean(module);
   }
}
