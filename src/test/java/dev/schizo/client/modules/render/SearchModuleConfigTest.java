package dev.schizo.client.modules.render;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import dev.schizo.client.ClientAccess;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SearchModuleConfigTest {
   @BeforeAll
   static void bootstrapMinecraftRegistries() {
      SharedConstants.tryDetectVersion();
      Bootstrap.bootStrap();
   }

   @Test
   void entitySearchSettingLoadsWithoutAnActiveWorld() {
      assertNull(ClientAccess.schizoMinecraft.level);

      SearchModule module = new SearchModule();
      JsonObject settings = new JsonObject();
      settings.addProperty("SearchEntities", true);
      JsonObject root = new JsonObject();
      root.add("settings", settings);

      assertDoesNotThrow(() -> module.fromJson(root));

      assertTrue(module.getLastConfigLoadIssues().isEmpty());
      JsonObject serializedSettings = module.toJson().getAsJsonObject("settings");
      assertTrue(serializedSettings.get("SearchEntities").getAsBoolean());
   }
}
