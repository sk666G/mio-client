package dev.schizo.client.modules;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.schizo.client.settings.BooleanSetting;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModuleConfigFieldTest {
   @Test
   void malformedTopLevelFieldDoesNotDiscardValidSetting() {
      ConfigModule module = new ConfigModule();
      JsonObject settings = new JsonObject();
      settings.addProperty("Flag", true);
      JsonObject root = new JsonObject();
      root.add("settings", settings);
      root.add("drawn", new JsonObject());

      assertDoesNotThrow(() -> module.fromJson(root));

      assertTrue(module.flag.getValue());
      assertTrue(module.isDrawn());
      assertEquals(
         List.of("drawn"),
         module.getLastConfigLoadIssues().stream()
            .map(dev.schizo.client.config.ModuleConfigLoadReport.Issue::settingName)
            .toList()
      );
   }

   @Test
   void malformedBindingAndEnableFieldsAreReportedIndependently() {
      ConfigModule module = new ConfigModule();
      JsonObject root = new JsonObject();
      root.add("key", new JsonArray());
      root.add("mouse", new JsonObject());
      root.add("enabled", new JsonArray());

      assertDoesNotThrow(() -> module.fromJson(root));

      assertTrue(module.getKeybind().isUnbound());
      assertFalse(module.isToggled());
      assertEquals(
         List.of("key", "mouse", "enabled"),
         module.getLastConfigLoadIssues().stream()
            .map(dev.schizo.client.config.ModuleConfigLoadReport.Issue::settingName)
            .toList()
      );
   }

   private static final class ConfigModule extends Module {
      private final BooleanSetting flag;

      private ConfigModule() {
         super("ConfigFieldTest", "Config field regression test", ModuleCategory.MISC);
         this.flag = this.add(new BooleanSetting("Flag", false));
      }
   }
}
