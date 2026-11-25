package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

class ContextProfileRegistryConfigTest {
   @Test
   void invalidRootPreservesExistingEnabledState() {
      ContextProfileRegistry registry = new ContextProfileRegistry();
      assertTrue(registry.setEnabled(true));
      assertTrue(registry.isEnabled());

      registry.fromJson(new JsonArray());

      assertTrue(registry.isEnabled());
   }

   @Test
   void unsupportedSchemaPreservesExistingEnabledState() {
      ContextProfileRegistry registry = new ContextProfileRegistry();
      assertTrue(registry.setEnabled(true));

      JsonObject root = new JsonObject();
      root.addProperty("version", ContextProfileRegistry.CURRENT_SCHEMA_VERSION + 1);
      root.addProperty("enabled", false);
      root.add("profiles", new JsonArray());

      registry.fromJson(root);

      assertTrue(registry.isEnabled());
   }

   @Test
   void malformedProfileListPreservesExistingEnabledState() {
      ContextProfileRegistry registry = new ContextProfileRegistry();
      assertTrue(registry.setEnabled(true));

      JsonObject root = new JsonObject();
      root.addProperty("version", ContextProfileRegistry.CURRENT_SCHEMA_VERSION);
      root.addProperty("enabled", false);
      root.add("profiles", new JsonObject());

      registry.fromJson(root);

      assertTrue(registry.isEnabled());
   }
}
