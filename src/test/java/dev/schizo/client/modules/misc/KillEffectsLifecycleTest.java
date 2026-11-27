package dev.schizo.client.modules.misc;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class KillEffectsLifecycleTest {
   @BeforeAll
   static void bootstrapMinecraftRegistries() {
      SharedConstants.tryDetectVersion();
      Bootstrap.bootStrap();
   }

   @Test
   void disabledKillSaySettingsDoNotStartReloadWork() throws Exception {
      KillEffectsModule module = new KillEffectsModule();
      Object messages = messagesOf(module);
      Field generation = messages.getClass().getDeclaredField("generation");
      generation.setAccessible(true);

      long before = generation.getLong(messages);
      module.killSaySetting.setValue(true);
      module.pathSetting.setValue("disabled-test.txt");
      module.refreshSetting.setValue(true);

      assertEquals(before, generation.getLong(messages));
      assertFalse(module.refreshSetting.getValue());
   }

   private static Object messagesOf(KillEffectsModule module) throws Exception {
      Field field = KillEffectsModule.class.getDeclaredField("killSayMessages");
      field.setAccessible(true);
      return field.get(module);
   }
}
