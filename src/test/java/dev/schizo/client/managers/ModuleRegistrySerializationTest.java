package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.schizo.client.config.ModuleConfigSchema;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModuleRegistrySerializationTest {
   @Test
   void oneModuleSerializationFailureAbortsWholeSnapshot() {
      Module good = new StaticJsonModule("Good");
      Module failing = new FailingJsonModule();

      IllegalStateException failure = assertThrows(
         IllegalStateException.class,
         () -> ModuleRegistry.serializeModules(List.of(good, failing))
      );

      assertTrue(failure.getMessage().contains("Failing"));
      assertInstanceOf(IllegalArgumentException.class, failure.getCause());
   }

   @Test
   void successfulSnapshotContainsMetadataAndModulePayload() {
      JsonObject serialized = ModuleRegistry.serializeModules(List.of(new StaticJsonModule("Good")));

      assertEquals(ModuleConfigSchema.CURRENT_VERSION, serialized.get("version").getAsInt());
      assertEquals("schizo", serialized.get("client").getAsString());
      assertEquals("all", serialized.get("category").getAsString());
      assertTrue(serialized.getAsJsonObject("Good").get("ok").getAsBoolean());
   }

   private static final class StaticJsonModule extends Module {
      private StaticJsonModule(String name) {
         super(name, "serialization test", ModuleCategory.MISC);
      }

      @Override
      public JsonElement toJson() {
         JsonObject json = new JsonObject();
         json.addProperty("ok", true);
         return json;
      }
   }

   private static final class FailingJsonModule extends Module {
      private FailingJsonModule() {
         super("Failing", "serialization failure test", ModuleCategory.MISC);
      }

      @Override
      public JsonElement toJson() {
         throw new IllegalArgumentException("boom");
      }
   }
}
