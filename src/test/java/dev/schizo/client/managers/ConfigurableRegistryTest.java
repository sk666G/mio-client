package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.schizo.client.settings.JsonConfigurable;
import org.junit.jupiter.api.Test;

class ConfigurableRegistryTest {
   @Test
   void acceptsTopLevelAndNestedJsonNames() {
      assertEquals("modules.json", ConfigurableRegistry.requireValidConfigName(configurable("modules.json")));
      assertEquals("presets/auto.json", ConfigurableRegistry.requireValidConfigName(configurable("presets/auto.json")));
   }

   @Test
   void rejectsBlankTraversalAbsoluteAndNonJsonNames() {
      for (String invalid : new String[] {"", " ", "../escape.json", "presets/../escape.json", "/absolute.json", "C:/absolute.json", "notes.txt"}) {
         assertThrows(IllegalStateException.class, () ->
            ConfigurableRegistry.requireValidConfigName(configurable(invalid)));
      }
   }

   private static JsonConfigurable configurable(String name) {
      return new JsonConfigurable() {
         @Override
         public void fromJson(JsonElement json) {
         }

         @Override
         public JsonElement toJson() {
            return new JsonObject();
         }

         @Override
         public String getConfigName() {
            return name;
         }
      };
   }
}
