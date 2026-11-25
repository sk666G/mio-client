package dev.schizo.client.settings;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RegistrySetSettingTest {
   @Test
   void stagesResolvedEntriesAndSkipsUnknownIdentifiers() {
      JsonArray json = new JsonArray();
      json.add("known");
      json.add("missing");
      json.add("known");

      Set<String> loaded = RegistrySetSetting.resolveJsonEntries(
         json,
         Map.of("known", "resolved")::get
      );

      assertEquals(Set.of("resolved"), loaded);
   }

   @Test
   void rejectsNonArrayRoots() {
      assertThrows(
         IllegalArgumentException.class,
         () -> RegistrySetSetting.resolveJsonEntries(new JsonObject(), value -> value)
      );
   }

   @Test
   void rejectsNonStringEntriesBeforePublication() {
      JsonArray json = new JsonArray();
      json.add("valid");
      json.add(42);

      assertThrows(
         IllegalArgumentException.class,
         () -> RegistrySetSetting.resolveJsonEntries(json, value -> value)
      );
   }

   @Test
   void wrapsResolverFailuresAsInvalidIdentifiers() {
      JsonArray json = new JsonArray();
      json.add("bad id");

      IllegalArgumentException failure = assertThrows(
         IllegalArgumentException.class,
         () -> RegistrySetSetting.resolveJsonEntries(
            json,
            value -> {
               throw new IllegalArgumentException("resolver failed");
            }
         )
      );

      assertTrue(failure.getMessage().contains("bad id"));
      assertNotNull(failure.getCause());
   }

   @Test
   void rejectsMissingResolver() {
      JsonArray json = new JsonArray();
      json.add("entry");

      assertThrows(
         IllegalArgumentException.class,
         () -> RegistrySetSetting.resolveJsonEntries(json, null)
      );
   }
}
