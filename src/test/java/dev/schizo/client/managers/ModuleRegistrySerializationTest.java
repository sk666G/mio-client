package dev.schizo.client.managers;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.schizo.client.modules.Module;
import dev.schizo.client.modules.ModuleCategory;
import org.junit.jupiter.api.Test;

class ModuleRegistrySerializationTest {
   @Test
   void successfulModuleSerializationPublishesTheWholeEntry() {
      JsonObject target = new JsonObject();
      Module module = new Module("Alpha", ModuleCategory.CLIENT);

      ModuleRegistry.addSerializedModule(target, module);

      assertTrue(target.has("Alpha"));
      assertTrue(target.get("Alpha").isJsonObject());
   }

   @Test
   void serializationFailureDoesNotPublishPartialEntry() {
      JsonObject target = new JsonObject();
      Module broken = new Module("Broken", ModuleCategory.CLIENT) {
         @Override
         public JsonElement toJson() {
            throw new IllegalStateException("boom");
         }
      };

      IllegalStateException failure = assertThrows(
         IllegalStateException.class,
         () -> ModuleRegistry.addSerializedModule(target, broken)
      );

      assertTrue(failure.getMessage().contains("Broken"));
      assertTrue(target.entrySet().isEmpty());
      assertNotNull(failure.getCause());
   }

   @Test
   void nullSerializedModuleIsRejectedWithoutPublication() {
      JsonObject target = new JsonObject();
      Module broken = new Module("NullConfig", ModuleCategory.CLIENT) {
         @Override
         public JsonElement toJson() {
            return null;
         }
      };

      assertThrows(
         IllegalStateException.class,
         () -> ModuleRegistry.addSerializedModule(target, broken)
      );

      assertTrue(target.entrySet().isEmpty());
   }

   @Test
   void rejectsMissingInputs() {
      Module module = new Module("Alpha", ModuleCategory.CLIENT);

      assertThrows(IllegalArgumentException.class, () -> ModuleRegistry.addSerializedModule(null, module));
      assertThrows(IllegalArgumentException.class, () -> ModuleRegistry.addSerializedModule(new JsonObject(), null));
   }
}
