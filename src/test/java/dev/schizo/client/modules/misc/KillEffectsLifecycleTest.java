package dev.schizo.client.modules.misc;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.events.MinecraftDisconnectEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
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

   @Test
   void disableClearsTrackedStateAndInvalidatesDelayedKillSayWork() throws Exception {
      KillEffectsModule module = new KillEffectsModule();
      Object messages = messagesOf(module);
      Field generation = messages.getClass().getDeclaredField("generation");
      generation.setAccessible(true);
      long beforeGeneration = generation.getLong(messages);

      Map<UUID, Integer> tracked = trackedTargetsOf(module);
      Collection<UUID> expired = expiredTargetsOf(module);
      UUID target = UUID.randomUUID();
      tracked.put(target, 12);
      expired.add(target);
      setKillStreak(module, 4);

      module.onDisable();

      assertTrue(tracked.isEmpty());
      assertTrue(expired.isEmpty());
      assertEquals(0, killStreakOf(module));
      assertEquals(beforeGeneration + 1L, generation.getLong(messages));
   }

   @Test
   void disconnectClearsTrackedStateAndInvalidatesDelayedKillSayWork() throws Exception {
      KillEffectsModule module = new KillEffectsModule();
      Object messages = messagesOf(module);
      Field generation = messages.getClass().getDeclaredField("generation");
      generation.setAccessible(true);
      long beforeGeneration = generation.getLong(messages);

      Map<UUID, Integer> tracked = trackedTargetsOf(module);
      Collection<UUID> expired = expiredTargetsOf(module);
      UUID target = UUID.randomUUID();
      tracked.put(target, 20);
      expired.add(target);
      setKillStreak(module, 3);

      Method disconnect = KillEffectsModule.class.getDeclaredMethod(
         "onMinecraftDisconnectEvent",
         MinecraftDisconnectEvent.class
      );
      disconnect.setAccessible(true);
      disconnect.invoke(module, new MinecraftDisconnectEvent());

      assertTrue(tracked.isEmpty());
      assertTrue(expired.isEmpty());
      assertEquals(0, killStreakOf(module));
      assertEquals(beforeGeneration + 1L, generation.getLong(messages));
   }

   @SuppressWarnings("unchecked")
   private static Map<UUID, Integer> trackedTargetsOf(KillEffectsModule module) throws Exception {
      Field field = KillEffectsModule.class.getDeclaredField("trackedTargets");
      field.setAccessible(true);
      return (Map<UUID, Integer>)field.get(module);
   }

   @SuppressWarnings("unchecked")
   private static Collection<UUID> expiredTargetsOf(KillEffectsModule module) throws Exception {
      Field field = KillEffectsModule.class.getDeclaredField("expiredTargets");
      field.setAccessible(true);
      return (Collection<UUID>)field.get(module);
   }

   private static int killStreakOf(KillEffectsModule module) throws Exception {
      Field field = KillEffectsModule.class.getDeclaredField("killStreak");
      field.setAccessible(true);
      return field.getInt(module);
   }

   private static void setKillStreak(KillEffectsModule module, int value) throws Exception {
      Field field = KillEffectsModule.class.getDeclaredField("killStreak");
      field.setAccessible(true);
      field.setInt(module, value);
   }

   private static Object messagesOf(KillEffectsModule module) throws Exception {
      Field field = KillEffectsModule.class.getDeclaredField("killSayMessages");
      field.setAccessible(true);
      return field.get(module);
   }
}
