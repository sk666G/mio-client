package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

class ContextProfilePresetPolicyTest {
   @Test
   void removesModuleActivationButPreservesSettingsAndMetadata() {
      JsonObject input = new JsonObject();
      input.addProperty("client", "schizo");
      input.addProperty("category", "modules");
      JsonObject module = new JsonObject();
      module.addProperty("enabled", true);
      JsonObject settings = new JsonObject();
      settings.addProperty("enabled", false);
      settings.addProperty("speed", 2.0);
      module.add("settings", settings);
      input.add("speed", module);

      JsonObject result = ContextProfilePresetPolicy.withoutModuleEnabledState(input.deepCopy()).getAsJsonObject();

      assertEquals("schizo", result.get("client").getAsString());
      assertEquals("modules", result.get("category").getAsString());
      assertFalse(result.getAsJsonObject("speed").has("enabled"));
      assertFalse(result.getAsJsonObject("speed").getAsJsonObject("settings").get("enabled").getAsBoolean());
      assertEquals(2.0, result.getAsJsonObject("speed").getAsJsonObject("settings").get("speed").getAsDouble());
   }

   @Test
   void ignoresNonObjectState() {
      assertEquals("scalar", ContextProfilePresetPolicy.withoutModuleEnabledState(
         new com.google.gson.JsonPrimitive("scalar")
      ).getAsString());
   }
}
