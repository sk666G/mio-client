package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import dev.schizo.client.ClientAccess;
import dev.schizo.client.events.ConnectScreenConnectEvent;
import dev.schizo.client.events.MinecraftDisconnectEvent;
import dev.schizo.client.settings.options.PresetCategory;
import dev.schizo.client.utils.network.AutoPresetServerEntry;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AutoPresetRegistryTest {
   @Test
   void allPresetAppliesBeforeSpecificCategoriesRegardlessOfHashMapOrder() {
      AutoPresetServerEntry settings = new AutoPresetServerEntry("example.org");
      settings.getPresets().put(PresetCategory.VISUALS, "visual");
      settings.getPresets().put(PresetCategory.ALL, "base");
      settings.getPresets().put(PresetCategory.MODULES, "modules");

      List<Map.Entry<PresetCategory, String>> ordered = AutoPresetRegistry.orderedPresetEntries(settings);

      assertEquals(
         List.of(PresetCategory.ALL, PresetCategory.MODULES, PresetCategory.VISUALS),
         ordered.stream().map(Map.Entry::getKey).toList()
      );
      assertEquals(List.of("base", "modules", "visual"), ordered.stream().map(Map.Entry::getValue).toList());
   }

   @Test
   void disconnectAllowsSameServerToBeTreatedAsANewConnection() throws Exception {
      AutoPresetRegistry registry = new AutoPresetRegistry();
      try {
         registry.onConnectScreenConnectEvent(new ConnectScreenConnectEvent("Example.org"));
         assertEquals("Example.org", readLastServer(registry));

         ClientAccess.eventBus.post(new MinecraftDisconnectEvent());
         assertNull(readLastServer(registry));

         registry.onConnectScreenConnectEvent(new ConnectScreenConnectEvent("Example.org"));
         assertEquals("Example.org", readLastServer(registry));
      } finally {
         ClientAccess.eventBus.unsubscribe(registry);
      }
   }

   @Test
   @SuppressWarnings({"rawtypes", "unchecked"})
   void malformedRawPresetEntriesAreIgnored() {
      AutoPresetServerEntry settings = new AutoPresetServerEntry("example.org");
      Map raw = settings.getPresets();
      raw.put("not-a-category", "bad");
      raw.put(PresetCategory.ALL, 123);
      raw.put(PresetCategory.HUD, "hud");

      List<Map.Entry<PresetCategory, String>> ordered = AutoPresetRegistry.orderedPresetEntries(settings);

      assertEquals(1, ordered.size());
      assertEquals(PresetCategory.HUD, ordered.getFirst().getKey());
      assertEquals("hud", ordered.getFirst().getValue());
   }
   private static String readLastServer(AutoPresetRegistry registry) throws Exception {
      Field field = AutoPresetRegistry.class.getDeclaredField("lastServer");
      field.setAccessible(true);
      return (String)field.get(registry);
   }

}
