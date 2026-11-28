package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

class ChatFilterRegistryTest {
   @Test
   void reloadReplacesRemovedFilters() {
      ChatFilterRegistry registry = new ChatFilterRegistry();
      registry.add("old", "old phrase");

      JsonObject root = new JsonObject();
      JsonArray filters = new JsonArray();
      JsonObject replacement = new JsonObject();
      replacement.addProperty("id", "new");
      replacement.addProperty("filter", "new phrase");
      filters.add(replacement);
      root.add("filters", filters);

      registry.fromJson(root);

      assertEquals(1, registry.size());
      assertTrue(registry.findById("new").isPresent());
      assertTrue(registry.findById("old").isEmpty());
   }

   @Test
   void duplicateIdsAreCollapsedCaseInsensitively() {
      ChatFilterRegistry registry = new ChatFilterRegistry();

      JsonObject root = new JsonObject();
      JsonArray filters = new JsonArray();
      JsonObject first = new JsonObject();
      first.addProperty("id", "Spam");
      first.addProperty("filter", "one");
      JsonObject duplicate = new JsonObject();
      duplicate.addProperty("id", "spam");
      duplicate.addProperty("filter", "two");
      filters.add(first);
      filters.add(duplicate);
      root.add("filters", filters);

      registry.fromJson(root);

      assertEquals(1, registry.size());
      assertEquals("one", registry.findById("SPAM").orElseThrow().filter());
   }

   @Test
   void malformedRootDoesNotDiscardExistingFilters() {
      ChatFilterRegistry registry = new ChatFilterRegistry();
      registry.add("keep", "phrase");

      assertThrows(IllegalArgumentException.class, () -> registry.fromJson(new JsonObject()));

      assertTrue(registry.findById("keep").isPresent());
      assertEquals(1, registry.size());
   }
}
