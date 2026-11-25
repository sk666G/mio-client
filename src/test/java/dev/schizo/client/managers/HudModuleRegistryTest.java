package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

class HudModuleRegistryTest {
   @Test
   void readsHudIndexFromValidConfiguration() {
      JsonObject hud = new JsonObject();
      hud.addProperty("index", 7);
      JsonObject module = new JsonObject();
      module.add("hud", hud);

      assertEquals(7, HudModuleRegistry.readHudIndex(module));
   }

   @Test
   void malformedHudIndexFallsBackWithoutThrowing() {
      JsonObject missingHud = new JsonObject();
      assertEquals(0, HudModuleRegistry.readHudIndex(missingHud));

      JsonObject hud = new JsonObject();
      hud.addProperty("index", "not-a-number");
      JsonObject module = new JsonObject();
      module.add("hud", hud);

      assertEquals(0, HudModuleRegistry.readHudIndex(module));
      assertEquals(0, HudModuleRegistry.readHudIndex(null));
   }
}
