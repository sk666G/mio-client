package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

class AliasRegistryTest {
   @Test
   void reloadReplacesRemovedPlayerAliases() {
      AliasRegistry registry = new AliasRegistry();
      registry.setPlayerAlias("OldPlayer", "Old Alias");

      JsonObject root = new JsonObject();
      JsonObject players = new JsonObject();
      players.addProperty("NewPlayer", "New Alias");
      root.add("players", players);

      registry.fromJson(root);

      assertEquals("OldPlayer", registry.getPlayerAlias("OldPlayer"));
      assertEquals("New Alias", registry.getPlayerAlias("NewPlayer"));
   }

   @Test
   void malformedRootDoesNotDiscardExistingAliases() {
      AliasRegistry registry = new AliasRegistry();
      registry.setPlayerAlias("Player", "Alias");

      assertThrows(IllegalArgumentException.class, () -> registry.fromJson(new JsonArray()));

      assertEquals("Alias", registry.getPlayerAlias("Player"));
   }
}
