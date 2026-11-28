package dev.schizo.client.modules.render;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class TunnelsLifecycleTest {
   @Test
   void sessionResetInvalidatesRuntimeGenerationAndDropsLevelOwner() throws Exception {
      TunnelsModule module = new TunnelsModule();
      long before = (Long)getField(module, "runtimeGeneration");

      module.resetSessionState();

      long after = (Long)getField(module, "runtimeGeneration");
      assertEquals(before + 1L, after);
      assertNull(getField(module, "scanLevel"));
   }

   private static Object getField(Object target, String name) throws Exception {
      Field field = target.getClass().getDeclaredField(name);
      field.setAccessible(true);
      return field.get(target);
   }
}
